/* DataBinder.java

{{IS_NOTE
	Purpose:
		
	Description:
		
	History:
		Wed Nov 15 14:24:25     2006, Created by Henri Chen.
}}IS_NOTE

Copyright (C) 2006 Potix Corporation. All Rights Reserved.

{{IS_RIGHT
}}IS_RIGHT
*/
package org.zkoss.zkplus.databind;

import org.zkoss.zk.ui.Path;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.UiException;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ComponentCtrl;
import org.zkoss.zk.ui.metainfo.Annotation; 

import org.zkoss.zul.impl.InputElement;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Slider;
import org.zkoss.zul.Calendar;
import org.zkoss.zul.ListitemRenderer;

import org.zkoss.util.ModificationException;
import org.zkoss.lang.Classes;
import org.zkoss.lang.Objects;
import org.zkoss.lang.reflect.Fields;

import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.lang.reflect.Method;

/**
 * The DataBinder used for binding ZK UI component and the backend data bean.
 *
 * @author Henri Chen
 */
public class DataBinder {
	private Map _bindings = new LinkedHashMap(29); //(comp, MAP(attr, Binding))
	private Map _beans = new HashMap(5); //(beanid, bean)
	private Map _eventComponentMap = new HashMap(255); //(cloned target, MAP(template, cloned dataTarget))
	private Set _targets = new HashSet(); //concerned template event triggering targets
	private Set _dataTargets = new HashSet(); //concerned template event applied data targets
	
	private boolean _init; //whether this databinder is initialized. 
							//Databinder is init automatically when saveXXX or loadXxx is called
	private static Map _converterMap = new HashMap(5); //(converterClassName, converter)
	private static final String VAR = "org.koss.zkplus.databind.VAR"; //the template variable name
	private static final String INDEXITEM = "org.koss.zkplus.databind.INDEXITEM"; //the listitem with index
	private static final String TEMPLATE = "org.koss.zkplus.databind.TEMPLATE"; //the template
	private static final String ISTEMPLATE = "org.koss.zkplus.databind.ISTEMPLATE"; //whether a template
	private static final Object NA = new Object();
	
	/** Binding bean to UI component. This is the same as addBinding(comp, attr, value, null, null). 
	 * @param comp The component to be associated.
	 * @param attr The attribute of the component to be associated.
	 * @param value The expression to associate the data bean.
	 */
	public void addBinding(Component comp, String attr, String value) {
		addBinding(comp, attr, value, null, null, null, null);
	}

	/** Binding bean to UI component. 
	 * @param comp The component to be associated.
	 * @param attr The attribute of the component to be associated.
	 * @param value The expression to associate the data bean.
	 * @param loadWhenEvents The event list when to load data.
	 * @param saveWhenEvent The event when to save data.
	 * @param access In the view of UI component: "load" load only, "both" load/save, "save" save only when doing
	 * data binding. null means using the default access natural of the component. e.g. Label.value is "load", 
	 * but Textbox.value is "both".
	 * @param converter The converter class used to convert classes between component and the associated bean.
	 * null means using the default class conversion method.
	 */
	public void addBinding(Component comp, String attr, String value,
		String[] loadWhenEvents, String saveWhenEvent, String access, String converter) {
		//handle default-bind defined in lang-addon.xml
		Object[] objs = loadPropertyAnnotation(comp, attr, "default-bind");
		/* logically impossible to hold "value" in default binding 
		if (value == null) {
			value = (String) objs[0];
		}
		if (loadWhenEvents == null) {
			loadWhenEvents = (String[]) objs[1];
		}
		*/
		if (saveWhenEvent == null) {
			saveWhenEvent = (String) objs[2];
		}
		if (access == null) {
			access = (String) objs[3];
		}
		if (converter == null) {
			converter = (String) objs[4];
		}
		Map attrMap = (Map) _bindings.get(comp);
		if (attrMap == null) {
			attrMap = new LinkedHashMap(3);
			_bindings.put(comp, attrMap);
		}
		if (attrMap.containsKey(attr)) { //override and merge
			final Binding binding = (Binding) attrMap.get(attr);
			binding.setValue(value);
			binding.addLoadWhenEvents(loadWhenEvents);
			binding.setSaveWhenEvent(saveWhenEvent);
			binding.setAccess(access);
			binding.setConverter(converter);
		} else {
			attrMap.put(attr, new Binding(attr, value, loadWhenEvents, saveWhenEvent, access, converter));
		}
	}
	
	/** Remove the binding associated with the attribute of the component.
	 * @param comp The component to be removed the data binding association.
	 * @param attr The attribute of the component to be removed the data binding association.
	 */
	public void removeBinding(Component comp, String attr) {
		Map attrMap = (Map) _bindings.get(comp);
		if (attrMap != null) {
			attrMap.remove(attr);
		}
	}

	//[0] value, [1] loadWhenEvents, [2] saveWhenEvent, [3] access, [4] converter
	protected Object[] loadPropertyAnnotation(Component comp, String propName, String bindName) {
		ComponentCtrl compCtrl = (ComponentCtrl) comp;
		Annotation ann = compCtrl.getAnnotation(propName, bindName);
		if (ann != null) {
			final Map attrs = ann.getAttributes(); //(tag, tagExpr)
			String[] loadWhenEvents = null;
			String saveWhenEvent = null;
			String access = null;
			String converter = null;
			String expr = null;
			for (final Iterator it = attrs.entrySet().iterator(); it.hasNext();) {
				Map.Entry entry = (Map.Entry) it.next();
				String tag = (String) entry.getKey();
				String tagExpr = (String) entry.getValue();
	
				if ("save-when".equals(tag)) {
					saveWhenEvent = tagExpr;
				} else if ("access".equals(tag)) {
					access = tagExpr;
				} else if ("converter".equals(tag)) {
					converter = tagExpr;
				} else if ("load-when".equals(tag)) {
					loadWhenEvents = parseExpression(tagExpr, ",");
				} else if ("value".equals(tag)) {
					expr = tagExpr;
				}
			}
			return new Object[] {expr, loadWhenEvents, saveWhenEvent, access, converter};
		}
		return new Object[5];
	}
	
	/** Bind a real bean object to the specified beanid. You might not need to call this method because this
	 * DataBinder would look up the variable via the {@link org.zkoss.zk.ui.Component#getVariable} method
	 * if it cannot find the specified bean via the given beanid.
	 *
	 * @param beanid The bean id used in data binding.
	 * @param bean The real bean object to be associated with the bean id.
	 */
	public void bindBean(String beanid, Object bean) {
		_beans.put(beanid, bean);
	}

	/** Load value from the data bean property to a specified attribute of the UI component.
	 * @param comp the UI component to be loaded value.
	 * @param attr the UI component attribute to be loaded value.
	 */
	public void loadAttribute(Component comp, String attr) {
		//skip detached component
		if (comp.getPage() != null) {
			init();
			final Component template = getTemplateComponent(comp);
			Map attrMap = (Map) (template != null ? _bindings.get(template) : _bindings.get(comp));
			if (attrMap != null) {
				Binding binding = (Binding) attrMap.get(attr);
				if (binding != null) {
					binding.loadAttribute(comp);
				}
			}
		}
	}			

	/** Save value from a specified attribute of the UI component to a data bean property.
	 * @param comp the UI component used to save value into backend data bean.
	 * @param attr the UI component attribute used to save value into backend data bean.
	 */
	public void saveAttribute(Component comp, String attr) {
		//skip detached component
		if (comp.getPage() != null) {
			init();
			final Component template = getTemplateComponent(comp);
			Map attrMap = (Map) (template != null ? _bindings.get(template) : _bindings.get(comp));
			if (attrMap != null) {
				Binding binding = (Binding) attrMap.get(attr);
				if (binding != null) {
					binding.saveAttribute(comp);
				}
			}
		}
	}
	
	/** Load values from the data bean properties to all attributes of a specified UI component. 
	 * @param comp the UI component to be loaded value.
	 */
	public void loadComponent(Component comp) {
		//skip detached component
		if (comp.getPage() != null) {
			init();
			final Component template = getTemplateComponent(comp);
			Map attrMap = (Map) (template != null ? _bindings.get(template) : _bindings.get(comp));
			if (attrMap != null) {
				loadAttrs(comp, attrMap.values());
			}
			
			//load kids of this component
			for(final Iterator it = comp.getChildren().iterator(); it.hasNext();) {
				loadComponent((Component) it.next()); //recursive
			}
		}
	}
	
	/** Save values from all attributes of a specified UI component to data bean properties. 
	 * @param comp the UI component used to save value into backend data bean.
	 */
	public void saveComponent(Component comp) {
		//skip detached component
		if (comp.getPage() != null) {
			init();
			final Component template = getTemplateComponent(comp);
			Map attrMap = (Map) (template != null ? _bindings.get(template) : _bindings.get(comp));
			if (attrMap != null) {
				saveAttrs(comp, attrMap.values());
			}

			//save kids of this component
			for(final Iterator it = comp.getChildren().iterator(); it.hasNext();) {
				loadComponent((Component) it.next()); //recursive
			}
		}
	}
	
	/** Load all value from data beans to UI components. */
	public void loadAll() {
		init();
		for (final Iterator it = _bindings.keySet().iterator(); it.hasNext(); ) {
			final Component comp = (Component) it.next();
			if (isTemplate(comp)) { //do via model and renderer, so skip
				continue;
			}
			loadComponent(comp);
		}
	}
	
	/** Save all values from UI components to beans. */
	public void saveAll() {
		init();
		for (final Iterator it = _bindings.keySet().iterator(); it.hasNext(); ) {
			final Component comp = (Component) it.next();
			if (isTemplate(comp)) { //do via model and renderer, so skip
				continue;
			}
			saveComponent(comp);
		}
	}

	private void loadAttrs(Component comp, Collection attrs) {
		for(final Iterator it = attrs.iterator(); it.hasNext();) {
			Binding binding = (Binding) it.next();
			if (binding.getAttr().startsWith("_")) { //control attribute, skip
				continue;
			}
			binding.loadAttribute(comp);
		}
	}	

	private void saveAttrs(Component comp, Collection attrs) {
		for(final Iterator it = attrs.iterator(); it.hasNext();) {
			Binding binding = (Binding) it.next();
			if (binding.getAttr().startsWith("_")) { //control attribute, skip
				continue;
			}
			binding.saveAttribute(comp);
		}
	}

	//late init
	protected void init() {
		if (!_init) {
			_init = true;
			List detachs = new ArrayList(_bindings.size());
			for(final Iterator it = _bindings.keySet().iterator(); it.hasNext();) {
				final Component comp = (Component) it.next();
				final Map attrMap = (Map) _bindings.get(comp);
				
				//_var special case; meaning a template component
				if (attrMap.containsKey("_var")) {
					comp.setAttribute(VAR, ((Binding)attrMap.get("_var")).getBeanid());
					setupTemplate(comp, Boolean.TRUE);
					setupRenderer(comp);
					detachs.add(comp);
				}

				//register event handler
				registerEvents(comp, attrMap.values());
			}
			
			for(final Iterator it = detachs.iterator(); it.hasNext(); ) {
				final Component comp = (Component) it.next();
				comp.detach();
			}
		}
	}
	
	private void registerEvents(Component comp, Collection attrs) {
		for(final Iterator it = attrs.iterator(); it.hasNext();) {
			Binding binding = (Binding) it.next();
			binding.registerSaveEventListener(comp);
			binding.registerLoadEventListeners(comp);
		}
	}	

//vv----------------------------------------
//:TODO: The following code is Component type tightly coupled, should change to use interface...

	//20061205, Henri Chen: Tightly coupled with Component type
	private void setupRenderer(Component template) {
		if (template instanceof Listitem) {
			final Listitem li = (Listitem) template;
			final Listbox lbx = li.getListbox();
			if (lbx.getItemRenderer() == null) {
				lbx.setItemRenderer(new Renderer(li));
			}
		}
	}
	
	//20061205, Henri Chen: Tightly coupled with Component type
	//get index of the specified component, if not a indexitem, return -1.
	private Object getListModelItem(Component comp, String beanid) {
		final Component indexitem = (Component) comp.getAttribute(INDEXITEM);
		if (indexitem != null && indexitem instanceof Listitem) {
			final Listitem li = (Listitem) indexitem;
			final String var = (String)getTemplateComponent(li).getAttribute(VAR);
			if (beanid.equals(var)) {
				final Listbox lbx = li.getListbox();
				final ListModel model = lbx.getModel();
				final int index = li.getIndex();
				return model.getElementAt(index);
			} else {
				return NA;
			}
		}
		throw new UiException("Unsupported collection item component for DataBinder: "+comp);
	}
	
//^^----------------------------------------------
	//get associated template component of a cloned component
	private Component getTemplateComponent(Component comp) {
		return (Component) comp.getAttribute(TEMPLATE);
	}
	
	//set associated template component of a cloned component
	private void setTemplateComponent(Component comp, Component template) {
		comp.setAttribute(TEMPLATE, template);
	}

	//whether a cloned item
	private boolean isClone(Component comp) {
		return getTemplateComponent(comp) != null;
	}

	//whether the specified component a template component
	private boolean isTemplate(Component comp) {
		return comp.getAttribute(ISTEMPLATE) == Boolean.TRUE;
	}

	//set the specified comp and its decendents to be template (or not)
	private void setupTemplate(Component comp, Boolean b) {
		comp.setAttribute(ISTEMPLATE, b);
		List kids = comp.getChildren();
		for(final Iterator it = kids.iterator(); it.hasNext(); ) {
			setupTemplate((Component) it.next(), b); //recursive
		}
	}

	//given cloned event target and template data target, get cloned data target
	private Component lookupDataTarget(Component target, Component dataTarget) {
		if (!_eventComponentMap.containsKey(target)) {
			return dataTarget;
		}
		final Map dataTargetMap = (Map)_eventComponentMap.get(target);
		if (!dataTargetMap.containsKey(dataTarget)) {
			return dataTarget;
		}
		return (Component) dataTargetMap.get(dataTarget); 
	}
	
	//parse token and return as a String[]
	protected String[] parseExpression(String expr, String seperator) {
		List list = myParseExpression(expr, seperator);
		if (list == null) {
			return null;
		}
		String[] results = new String[list.size()];
		int j = 0;
		for(final Iterator it = list.iterator(); it.hasNext(); ++j) {
			String result = (String) it.next();
			if (result != null) {
				result  = result.trim();
				if (result.length() == 0)
					result = null;
			}
			if (j == 0 && result == null) {
				return null;
			}
			results[j] = result;
		}
		return results;
	}
	
	private List myParseExpression(String expr, String separator) {
		if (expr == null) {
			return null;
		}
		List results = new ArrayList(5);
		while(true) {
			int j = expr.indexOf(separator);
			if (j < 0) {
				results.add(expr);
				return results;
			}
			results.add(expr.substring(0, j));

			if (expr.length() <= (j+1)) {
				return results;
			}
			expr = expr.substring(j+1);
		}
	}

	//assume no state is stored in TypeConverter
	private static TypeConverter lookupConverter(String clsName) {
		TypeConverter converter = (TypeConverter) _converterMap.get(clsName);
		if (converter == null) {
			try {
				converter = (TypeConverter) Classes.newInstanceByThread(clsName);
				_converterMap.put(clsName, converter);
			} catch (java.lang.reflect.InvocationTargetException ex) {
				throw UiException.Aide.wrap(ex);
			} catch (Exception ex) {
				throw UiException.Aide.wrap(ex);
			}
		}
		return converter;
	}
	
	//A binding association class.
	private class Binding {
		private String _attr;
		private String _value;
		private String _beanid;
		private String _props; //a.b.c
		private String[] _loadWhenEvents;
		private String _saveWhenEvent;
		private boolean _canLoad = true; //default access to "load"
		private boolean _canSave;
		private TypeConverter _converter;
		
		/** Construtor to form a binding between UI component and backend data bean.
		 * @param attr The attribute of the component to be associated.
		 * @param value The expression to associate the data bean.
		 * @param loadWhenEvents The event list when to load data.
		 * @param saveWhenEvent The event when to save data.
		 * @param access In the view of UI component: "load" load only, "both" load/save, "save" save only when doing
		 * data binding. null means using the default access natural of the component. e.g. Label.value is "load", 
		 * but Textbox.value is "both".
		 * @param converter The converter class used to convert classes between component and the associated bean.
		 * null means using the default class conversion method.
		 */
		private Binding(String attr, String value, 
			String[] loadWhenEvents, String saveWhenEvent, String access, String converter) {
				
			setAttr(attr);
			setValue(value);
			addLoadWhenEvents(loadWhenEvents);
			setSaveWhenEvent(saveWhenEvent);
			setAccess(access);
			setConverter(converter);
		}
		
		private void setAttr(String attr) {
			_attr = attr;
		}
		
		private void setValue(String value) {
			if (value != null) {
				String[] results = parseExpression(value);
				_beanid = results[0];
				_props = results[1];
			}
		}
		
		private void setSaveWhenEvent(String saveWhenEvent) {
			if (saveWhenEvent != null) {
				_saveWhenEvent = saveWhenEvent;
			}
		}
		
		private void addLoadWhenEvents(String[] loadWhenEvents) {
			if (loadWhenEvents == null) {
				return;
			}
			int sz1 = _loadWhenEvents == null ? 0 : _loadWhenEvents.length;
			int sz2 = loadWhenEvents.length;
			String[] merge = new String[sz1 + sz2];
			
			if (sz1 > 0) {
				System.arraycopy(_loadWhenEvents, 0, merge, 0, sz1);
			}
			if (sz2 > 0) {
				System.arraycopy(loadWhenEvents, 0, merge, sz1, sz2);
			}
			_loadWhenEvents = merge;
		}

		private void setAccess(String access) {
			if (access == null) { //default access to load
				return;
			}
			
			if ("both".equals(access)) {
				_canLoad = true;
				_canSave = true;
			} else if ("load".equals(access)) {
				_canLoad = true;
				_canSave = false;
			} else if ("save".equals(access)) {
				_canLoad = false;
				_canSave = true;
			} else if ("none".equals(access)) { //unknow access mode
				_canLoad = false;
				_canSave = false;
			} else {
				throw new UiException("Unknown DataBinder access mode. Should be \"both\", \"load\", \"save\", or \"none\": "+access);
			}
		}
		
		private void setConverter(String converter) {
			if (converter != null) {
				_converter = lookupConverter(converter);
			}
		}			
		
		private String getBeanid() {
			return _beanid;
		}
		
		private String getValue() {
			return _beanid+"."+_props;
		}
		
		private String getAttr() {
			return _attr;
		}
		
		//register events when to load component value from the bean
		private void registerLoadEventListeners(Component comp) {
			if (canLoad() && _loadWhenEvents != null) {
				for(int j = 0; j < _loadWhenEvents.length; ++j) {
					String expr = _loadWhenEvents[j];
					String[] results = parseExpression(expr); //[0] bean id or bean path, [1] event name
					Component target = (Component) ("self".equals(results[0]) ? comp : lookupBean(comp, results[0]));
					registerLoadEventListener(results[1], target, comp);
				}
			}
		}

		//register events when to save component value to the bean.
		private void registerSaveEventListener(Component comp) {
			if (canSave() && _saveWhenEvent != null) {
				String expr = _saveWhenEvent;
				String[] results = parseExpression(expr); //[0] bean id or bean path, [1] event name
				Component target = (Component) ("self".equals(results[0]) ? comp : lookupBean(comp, results[0]));
				registerSaveEventListener(results[1], target, comp);
			}
		}

		private void registerLoadEventListener(String eventName, Component target, Component dataTarget) {
			if (isTemplate(dataTarget)) {
				_targets.add(target); //concerned template event target
				_dataTargets.add(dataTarget); //concerned template event applied data target
			}
			target.addEventListener(eventName, new LoadEventListener(dataTarget));
		}

		private void registerSaveEventListener(String eventName, Component target, Component dataTarget) {
			if (isTemplate(dataTarget)) {
				_targets.add(target); //concerned template event target
				_dataTargets.add(dataTarget); //concerned template event applied data target
			}
			target.addEventListener(eventName, new SaveEventListener(dataTarget));
		}
		
		private boolean canLoad() {
			return _canLoad;
		}
		
		private boolean canSave() {
			return _canSave;
		}

		private void loadAttribute(Component comp) {
			try {
				Object bean = lookupBean(comp, _beanid);
				if (bean == null) {
					throw new UiException("Cannot find the specified bean: "+_beanid+" in "+getValue());
				}
				Object val = _props == null ? bean : Fields.getField(bean, _props);
				if (_converter != null) {
					val = _converter.coerceToUi(val);
				}
				Fields.setField(comp, _attr, val, _converter == null);
			} catch (ClassCastException ex) {
				throw UiException.Aide.wrap(ex);
			} catch (NoSuchMethodException ex) {
				throw UiException.Aide.wrap(ex);
			} catch (ModificationException ex) {
				throw UiException.Aide.wrap(ex);
			}
		}
					
		private void saveAttribute(Component comp) {
			try {
				Object bean = lookupBean(comp, _beanid);
				if (bean == null) {
					throw new UiException("Cannot find the specified bean: "+_beanid+" in "+getValue());
				}
				Object val = Fields.getField(comp, _attr);
				if (_converter != null) {
					val = _converter.coerceToBean(val);
				}
				if (_props == null && !isClone(comp)) { //assign back to where bean is stored
					if (_beans.containsKey(_beanid)) {
						_beans.put(_beanid, val);
					} else {
						comp.setVariable(_beanid, val, false);
					}
				} else {
					Fields.setField(bean, _props, val, _converter == null);
				}
			} catch (ClassCastException ex) {
				throw UiException.Aide.wrap(ex);
			} catch (NoSuchMethodException ex) {
				throw UiException.Aide.wrap(ex);
			} catch (ModificationException ex) {
				throw UiException.Aide.wrap(ex);
			}
		}

		private Object lookupBean(Component comp, String beanid) {
			//check collection template special case first
			Object bean = NA;
			if (isClone(comp)) {
				bean = getListModelItem(comp, beanid);
			}
			
			if (bean == NA) { //not available 
				//fetch the bean object
				if (_beans.containsKey(beanid)) {
					bean = _beans.get(beanid);
				} else if (beanid.startsWith("/")) { //a component Path in ID Space
					bean = Path.getComponent(beanid);	
				} else {
					bean = comp.getVariable(beanid, false);
				}
			}

			return bean;
		}

		private String[] parseExpression(String expr) {
			String beanid = null;
			String props = null;
			int j = expr.indexOf(".");
			if (j < 0) { //bean only
				beanid = expr;
				props = null;
			} else {
				beanid = expr.substring(0, j);
				props = expr.substring(j+1);
			}
			String[] results = new String[2];
			results[0] = beanid;
			results[1] = props;
			return results;
		}

		private abstract class BaseEventListener implements EventListener {
			protected Component _dataTarget;
			
			public BaseEventListener(Component dataTarget) {
				_dataTarget = dataTarget;
			}
			
/*			public Component getDataTarget() {
				return _dataTarget;
			}
*/			
			public boolean isAsap() {
				return true;
			}
		}
			
		private class LoadEventListener extends BaseEventListener {
			public LoadEventListener(Component dataTarget) {
				super(dataTarget);
			}
			public void onEvent(Event event) {
				Component dataTarget = lookupDataTarget((Component)event.getTarget(), _dataTarget);
				loadAttribute(dataTarget);
			}
		}

		private class SaveEventListener extends BaseEventListener {
			public SaveEventListener(Component dataTarget) {
				super(dataTarget);
			}
			public void onEvent(Event event) {
				Component dataTarget = lookupDataTarget(event.getTarget(), _dataTarget);
				saveAttribute(dataTarget);
			}
		}
	}

	private class Renderer implements org.zkoss.zul.ListitemRenderer {
		private Listitem _template;
		
		private Renderer(Listitem template) {
			_template = template;
		}
	    public void render(Listitem item, java.lang.Object bean) {
			//clone from template
	        final Listitem clone = (Listitem)_template.clone();
	        setupTemplate(clone, null); //not template for cloned component tree
	        
	        //copy children into item from clone
	        final List clonekids = clone.getChildren();
	        System.out.println("clonekids="+clonekids);
	        
	        item.getChildren().clear();
	        //addAll will cause kids to move parent and thus change clonekids, must make a copy
	        item.getChildren().addAll(new ArrayList(clonekids)); 
	        System.out.println("item.getChildren()="+item.getChildren());
	        
	        //setup the clone listitem and collect jump table for SaveEventListener and LoadEventListener
	        Map dataTargetMap = new HashMap(7);
	        List targetList = new ArrayList(7);
	        setupClone(item, _template, item, dataTargetMap, targetList);
	        
	        //setup jump table for template SaveEventListner and LoadEventListener
	        for(final Iterator it = targetList.iterator(); it.hasNext();) {
	        	final Object target = it.next();
	        	_eventComponentMap.put(target, dataTargetMap);
	        }

	        //apply the data binding
	        loadComponent(item);
	    }
	
		//link cloned to template, collection Save & Load Event mapping information, remove id
		private void setupClone(Component comp, Component template, Component item, Map dataTargetMap, List targetList) {
			if (_targets.contains(template)) {
				targetList.add(comp);
			}
			if (_dataTargets.contains(template)) {
				dataTargetMap.put(template, comp);
			}
			setTemplateComponent(comp, template);
			comp.setAttribute(INDEXITEM, item);
			comp.setId("@"+comp.getUuid()); //init id to @uuid to avoid duplicate id issue
			
	        //setup clone kids
	        final Iterator itt = template.getChildren().iterator();
	        final Iterator itc = comp.getChildren().iterator();
	        while (itt.hasNext()) {
	        	final Component t = (Component) itt.next();
	        	final Component c = (Component) itc.next();
	        	setupClone(c, t, item, dataTargetMap, targetList);	//recursive
	        }
	    }
	}
}
