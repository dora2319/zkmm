/* ComponentDefinition.java

{{IS_NOTE
	Purpose:
		
	Description:
		
	History:
		Tue May 31 17:54:45     2005, Created by tomyeh
}}IS_NOTE

Copyright (C) 2005 Potix Corporation. All Rights Reserved.

{{IS_RIGHT
	This program is distributed under GPL Version 2.0 in the hope that
	it will be useful, but WITHOUT ANY WARRANTY.
}}IS_RIGHT
*/
package org.zkoss.zk.ui.metainfo;

import java.util.Collection;
import java.util.Map;

import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.metainfo.impl.ComponentDefinitionImpl;
import org.zkoss.zk.ui.util.ComponentRenderer;

/**
 * A component definition.
 * Like class in Java, a {@link ComponentDefinition} defines the behavior
 * of a component.
 *
 * <p>The implementation need NOT to be thread safe, since the caller
 * has to {@link #clone} first if accessed concurrently.
 *
 * @author tomyeh
 */
public interface ComponentDefinition extends Cloneable {
	/** A special definition to represent the zk component. */
	public final static ComponentDefinition ZK =
		new ComponentDefinitionImpl(null, null, "zk", Component.class);;

	/** Returns the language definition, or null if it is a temporty definition
	 * belonging to a page.
	 */
	public LanguageDefinition getLanguageDefinition();

	/** Returns name of this component definition (never null).
	 * It is unique in the same language, {@link LanguageDefinition},
	 * if it belongs to a language, i.e.,
	 * {@link #getLanguageDefinition} is not null.
	 */
	public String getName();

	/** Returns the property name to which the text enclosed within
	 * the element (associated with this component definition) is assigned to.
	 *
	 * <p>Default: null (means to create a Label component as the child)
	 *
	 * <p>For example, if {@link #getTextAs} returns null, then
	 * a Label component is created as a child of <code>comp</code>
	 * with the "Hi Text" value in the following example:
	 *
	 *<pre><code>&lt;comp&gt;
	 *  Hi Text
	 *&lt;/comp&gt;</code></pre>
	 *
	 * <p>In other words, it is equivalent to
	 *
	 *<pre><code>&lt;comp&gt;
	 *  &lt;label value="Hi Text"/&gt;
	 *&lt;/comp&gt;</code></pre>
	 *
	 * <p>On the other hand, if {@link #getTextAs} returns a non-empty string,
	 * say, "content", then
	 * "Hi Text" is assigned to the content property of <code>comp</comp>.
	 * In other words, it is equivalent to
	 *
	 *<pre><code>&lt;comp content="Hi Text"/&gt;
	 *&lt;/comp&gt;</code></pre>
	 *
	 * <p>It is also the same as
	 *
	 *<pre><code>&lt;comp&gt;
	 *  &lt;attribute name="content"/&gt;
	 *  Hi Text
	 *  &lt;/attribute&gt;
	 *&lt;/comp&gt;</code></pre>
	 *
	 * <p>To enable it, you can declare <code>text-as</code> in
	 * the component definition in lang.xml or lang-addon.xml:
	 *
	 * <pre><code>&lt;component&gt;
	 *  &lt;component-name&gt;html&lt;/component-name&gt;
	 *  &lt;text-as&gt;content&lt;/text-as&gt;
	 *...</code></pre>
	 *
	 * @since 3.0.0
	 */
	public String getTextAs();

	/** Returns whether this is a macro component.
	 * @see #getMacroURI
	 */
	public boolean isMacro();
	/** Returns the macro URI, or null if not a macro.
	 */
	public String getMacroURI();
	/** Returns whether this is an inline macro.
	 * If false, you have to examine {@link #isMacro} to see whether it
	 * is a regular macro.
	 */
	public boolean isInlineMacro();

	/** Returns whether this is used for the native namespace.
	 *
	 * @since 3.0.0
	 * @see LanguageDefinition#getNativeDefinition
	 */
	public boolean isNative();
	/** Returns the class (Class) or the class name (String) that
	 * implements the component.
	 *
	 * <p>If a string is returned, the real class may depend on
	 * which page a component will be created to.
	 * Reason: the zscript interpreter is associated with a page and
	 * it may define classes upon evaluating a page.
	 */
	public Object getImplementationClass();
	/** Sets the class to implements the component.
	 *
	 * <p>Note: currently, classes specified in lang.xml or lang-addon.xml
	 * must be resolved when loading the files.
	 * However, classes specified in a page (by use of class or use attributes)
	 * might be resolved later because it might be defined by zscript.
	 */
	public void setImplementationClass(Class cls);
	/** Sets the class name to implements the component.
	 * Unlike {@link #setImplementationClass(Class)}, the class won't
	 * be resolved until {@link ComponentInfo#newInstance} or {@link #getImplementationClass}
	 * is used. In other words, the class can be provided later
	 * (thru, usually, zscript).
	 */
	public void setImplementationClass(String clsnm);
	/** Resolves and returns the class that implements the component.
	 *
	 * <p>Unlike {@link #getImplementationClass},
	 * this method will resolve a class name (String) to a class (Class),
	 * if necessary.
	 * In addition, if the clsnm argument is specified,
	 * it is used instead of {@link #getImplementationClass}.
	 * In other words, it overrides the default class.
	 *
	 * @param clsnm [optional] If specified, clsnm is used instead of
	 * {@link #getImplementationClass}.
	 * In other words, it overrides the default class.
	 * @param page the page to check whether the class is defined
	 * in its interpreters. Ignored if null.
	 * This method will search the class loader of the current thread.
	 * If not found, it will search the interpreters of the specifed
	 * page ({@link Page#getLoadedInterpreters}).
	 * Note: this method won't attach the component to the specified page.
	 * @exception ClassNotFoundException if the class not found
	 */
	public Class resolveImplementationClass(Page page, String clsnm)
	throws ClassNotFoundException;
	/** Returns whether a component belongs to this definition.
	 *
	 * <p>If {@link #resolveImplementationClass} failed to resolve,
	 * true is returned!
	 */
	public boolean isInstance(Component comp);

	/** Creates an component of this definition.
	 *
	 * <p>Note: this method doesn't invoke {@link #applyProperties}.
	 * It is caller's job to apply these properties if necessary.
	 * Since the value of a property might depend on the component tree,
	 * it is better to assign the component with a proper parent
	 * before calling {@link #applyProperties}.
	 *
	 * <p>Similarly, this method doesn't attach the component to the
	 * specified page. Developers may or may not add it to a page or
	 * a parent.
	 *
	 * <p>An application developer can invoke
	 * {@link org.zkoss.zk.ui.sys.UiFactory#newComponent}
	 * instead of {@link #newInstance}, since a deployer might
	 * customize the way to create components by providing
	 * an implementation of {@link org.zkoss.zk.ui.sys.UiFactory}.
	 * In additions, it also invokes {@link #applyProperties}
	 * assigning page/parent.
	 *
	 * <p>On the other hand, this method is 'low-level'. It simply resolves
	 * the implementation class by use of {@link #resolveImplementationClass},
	 * and then uses it to create an instance.
	 *
	 * @param clsnm [optional] If specified, clsnm is used instead of
	 * {@link #getImplementationClass}.
	 * In other words, it overrides the default class.
	 * @param page the page that is used to resolve the implementation
	 * class. It is used only this definition is associated
	 * with a class name by {@link #setImplementationClass(String)},
	 * or clsnm is not null.
	 * Note: this method won't attach the component to the specified page.
	 * It can be null if {@link #getImplementationClass} returns a Class
	 * instance, and clsnm is null.
	 * @return the new component (never null)
	 */
	public Component newInstance(Page page, String clsnm);

	/** Adds a mold based on an URI.
	 *
	 * @param moldURI an URI of the mold; never null nor empty.
	 * If it starts with "class:", the following substring is assumed to be
	 * the class name of {@link ComponentRenderer}, and then it invokes
	 * {@link addMold(String, ComponentRenderer).
	 * If not staring with "class:", it is pure an URI, and it may
	 * contain XEL expressions.
	 */
	public void addMold(String name, String moldURI);
	/** Adds a mold based on {@link ComponentRenderer}.
	 *
	 * @param renderer a component renderer. It is shared
	 * by all component instances belonging to this definition.
	 * @since 3.0.0
	 */
	public void addMold(String name, ComponentRenderer renderer);

	/** Adds a mold with an instance of {@link 
	/** Returns the URI (String) or an instance of {@link ComponentRenderer}
	 * of the mold, or null if no such mold available.
	 * In other words, if a String instance is returned, it is the URI
	 * of the mold. If a {@link ComponentRenderer}
	 * instance is returned, it is the object responsible to handle
	 * the generation of the component's output.
	 *
	 * <p>If the mold URI contains an expression, it will be evaluated first
	 * before returning.
	 *
	 * @param name the mold
	 * @return an URI in String, or a {@link ComponentRenderer},
	 * as of release 3.0.0
	 * @see org.zkoss.zk.ui.AbstractComponent#redraw
	 */
	public Object getMoldURI(Component comp, String name);
	/** Returns whether the specified mold exists.
	 */
	public boolean hasMold(String name);
	/** Returns a readonly collection of mold names supported by
	 * this definition.
	 */
	public Collection getMoldNames();

	/** Adds a property initializer.
	 * It will initialize a component when created with is definition.
	 *
	 * @param name the member name. The component must have a valid setter
	 * for it.
	 * @param value the value. It might contain expressions (${}).
	 */
	public void addProperty(String name, String value);
	/** Applies the properties and custom attributes defined in
	 * this definition to the specified component.
	 *
	 * <p>Note: annotations are applied to the component when a component
	 * is created. So, this method doesn't and need not to copy them.
	 */
	public void applyProperties(Component comp);
	/** Evaluates and retrieves properties to the specified map.
	 *
	 * @param propmap the map to store the retrieved properties.
	 * If null, a HashMap instance is created.
	 * (String name, Object value).
	 * @param owner the owner page; used if parent is null
	 * @param parent the parent
	 */
	public Map evalProperties(Map propmap, Page owner, Component parent);

	/** Returns the annotation map defined in this definition, or null
	 * if no annotation is ever defined.
	 */
	public AnnotationMap getAnnotationMap();

	/** Clones this definition and assins with the specified language
	 * definition and name.
	 */
	public ComponentDefinition clone(LanguageDefinition langdef, String name);

	/** Clones this component definition.
	 * You rarely invoke this method directly. Rather, use
	 * {@link #clone(LanguageDefinition, String)}.
	 *
	 * <p>Note: the caller usually has to change the component name,
	 * and then assign to a language definition ({@link LanguageDefinition})
	 * or a page definition ({@link PageDefinition}).
	 *
	 * @return the new component definition by cloning from this definition.
	 */
	public Object clone();
}
