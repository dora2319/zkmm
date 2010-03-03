/* Include.java

{{IS_NOTE
	Purpose:
		
	Description:
		
	History:
		Wed Sep 28 18:01:03     2005, Created by tomyeh
}}IS_NOTE

Copyright (C) 2005 Potix Corporation. All Rights Reserved.

{{IS_RIGHT
	This program is distributed under GPL Version 3.0 in the hope that
	it will be useful, but WITHOUT ANY WARRANTY.
}}IS_RIGHT
*/
package org.zkoss.zul;

import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.io.Writer;
import java.io.IOException;

import org.zkoss.lang.Library;
import org.zkoss.lang.Objects;
import org.zkoss.lang.Exceptions;
import org.zkoss.mesg.Messages;
import org.zkoss.util.logging.Log;

import org.zkoss.web.Attributes;

import org.zkoss.zk.mesg.MZk;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.Execution;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.UiException;
import org.zkoss.zk.ui.WrongValueException;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.sys.UiEngine;
import org.zkoss.zk.ui.sys.WebAppCtrl;
import org.zkoss.zk.ui.util.Clients;

import org.zkoss.zul.impl.XulElement;
import org.zkoss.zul.mesg.MZul;

/**
 * Includes the result generated by any servlet, not limited to a ZUML page.
 *
 * <p>Non-XUL extension.
 *
 * <p>Since 3.6.2, there are three modes: auto, instant and defer (default).
 * The behavior prior to 3.6.2 is the same as the defer mode.
 * To be fully backward compatible, the default mode is <code>defer</code>.
 * However, we recommend you change it by specifying a library variable named
 * <code>org.zkoss.zul.include.mode</code> to be <code>auto</code>,
 * since the auto mode is more intuitive and easier to handle.
 *
 * <h3>The instant mode</h3>
 *
 * <p>In the instant mode, the page to be included are loaded 'instantly'
 * with {@link Execution#createComponents} when {@link #afterCompose}
 * is called. Furthermore, the components are created as the child components
 * of this include component (like a macro component).
 *
 * <p>Notices:
 * <ul>
 * <li>The instant mode supports only ZUML pages.</li>
 * <li>The isntance mode doesn't support {@link #setProgressing} nor
 * {@link #setLocalized}</li>
 * </ul>
 *
 * <h3>The defer mode (default)</h3>
 *
 * <p>In the defer mode (the only mode supported by ZK prior to 3.6.2),
 * the page is included by servlet container (the <code>include</code> method
 * of <code>javax.servlet.RequestDispatcher</code>) in the render phase
 * (i.e., after all components are created). The page can be any
 * servlet; not limited to a ZUML page.
 *
 * <p>If it is eventually another ZUML page, a ZK page ({@link org.zkoss.zk.ui.Page})
 * is created and added to the current desktop.
 * You can access them only via inter-page API (see{@link org.zkoss.zk.ui.Path}).
 *
 * <h3>The auto mode</h3>
 *
 * <p>In the auto mode, the include component decides the mode based on
 * the name specified in the src property ({@link #setSrc}).
 * If <code>src</code> is ended with the extension named <code>.zul</code>
 * or <code>.zhtml</code>, the <code>instant</code> mode is assumed.
 * Otherwise, the <code>defer</code> mode is assumed.
 * Notice that if a query string is specified, the <code>defer</code> mode
 * is assumed, too.
 *
 * <p>Notice that invoking {@link #setProgressing} or {@link #setLocalized}
 * with true will imply the <code>defer</code> mode (if the mode is <code>auto</code>).
 *
 * <h3>Passing Parameters</h3>
 *
 * <p>There are two ways to pass parameters to the included page:
 * <p>First, since ZK 3.0.4,
 * you can use {@link #setDynamicProperty}, or, in ZUL,
 * <pre><code>&lt;include src="/WEB-INF/mypage" arg="something"/&gt;</code></pre>
 *
 * <p>Second, you can use the query string:
 * <pre><code>&lt;include src="/WEB-INF/mypage?arg=something"/&gt;</code></pre>
 *
 * <p>With the query string, you can pass only the String values.
 * and the parameter can be accessed by {@link Execution#getParameter}
 * or javax.servlet.ServletRequest's getParameter.
 * Or, you can access it with the param variable in EL expressions.
 *
 * <p>On the other hand, the dynamic properties ({@link #setDynamicProperty})
 * are passed to the included page thru the request's attributes
 * You can pass any type of objects you want.
 * In the included page, you can access them by use of
 * {@link Execution#getAttribute} or javax.servlet.ServletRequest's
 * getAttribute. Or, you can access with the requestScope variable
 * in EL expressions.
 *
 * <h3>Macro Component versus {@link Include}</h3>
 *
 * If the include component is in the instant mode, it is almost the same as
 * a macro component. On the other hand, if in the pag mode, they are different:
 * <ol>
 * <li>{@link Include} (in defer mode) could include anything include ZUML,
 * JSP or any other
 * servlet, while a macro component could embed only a ZUML page.</li>
 * <li>If {@link Include} (in defer mode) includes a ZUML page, a
 * {@link org.zkoss.zk.ui.Page} instance is created which is owned
 * by {@link Include}. On the other hand, a macro component makes
 * the created components as the direct children -- i.e.,
 * you can browse them with {@link org.zkoss.zk.ui.Component#getChildren}.</li>
 * <li>{@link Include} (in defer mode) creates components in the Rendering phase,
 * while a macro component creates components in {@link org.zkoss.zk.ui.HtmlMacroComponent#afterCompose}.</li>
 * <li>{@link Include#invalidate} (in defer mode) will cause it to re-include
 * the page (and then recreate the page if it includes a ZUML page).
 * However, {@link org.zkoss.zk.ui.HtmlMacroComponent#invalidate} just causes it to redraw
 * and update the content at the client -- like any other component does.
 To re-create, you have to invoke {@link org.zkoss.zk.ui.HtmlMacroComponent#recreate}.</li>
 * </ol>
 *
 * <p>In additions to macro and {@link Include}, you can use the fulfill
 * attribute as follows:
 * <code>&lt;div fulfill="=/my/foo.zul"&gt;...&lt;/div&gt;
 *
 * @author tomyeh
 * @see Iframe
 */
public class Include extends XulElement implements org.zkoss.zul.api.Include {
	private static final Log log = Log.lookup(Include.class);
	private String _src;
	private Map _dynams;
	private String _mode = getDefaultMode();
	private boolean _localized;
	private boolean _progressing;
	private boolean _afterComposed;
	private boolean _instantMode;
	/** 0: not yet handled, 1: wait for echoEvent, 2: done. */
	private byte _progressStatus;

	public Include() {
	}
	public Include(String src) {
		setSrc(src);
	}

	/**
	 * Sets whether to show the {@link MZul#PLEASE_WAIT} message before a long operation.
	 * This implementation will automatically use an echo event like {@link Events#echoEvent(String, org.zkoss.zk.ui.Component, String)} 
	 * to suspend the including progress before using the {@link Clients#showBusy(String, boolean)} 
	 * method to show the {@link MZul#PLEASE_WAIT} message at client side. 
	 * 
	 * <p>Default: false.
	 * @since 3.0.4
	 */
	public void setProgressing(boolean progressing) {
		if (_progressing != progressing) {
			if (progressing && "instant".equals(_mode))
				throw new UnsupportedOperationException("progressing not allowed in instant mode");

			_progressing = progressing;
			if (_progressing)
				fixMode(); //becomes defer mode if auto
			checkProgressing();
		}
	}
	/**
	 * Returns whether to show the {@link MZul#PLEASE_WAIT} message before a long operation.
	 * <p>Default: false.
	 * @since 3.0.4
	 */
	public boolean getProgressing() {
		return _progressing;
	}
	/**
	 * Internal use only.
	 *@since 3.0.4
	 */
	public void onEchoInclude() {
		Clients.showBusy(null , false);
		super.invalidate();
	}
	/** Returns the src.
	 * <p>Default: null.
	 */
	public String getSrc() {
		return _src;
	}
	/** Sets the src.
	 *
	 * <p>If src is changed, the whole component is invalidate.
	 * Thus, you want to smart-update, you have to override this method.
	 *
	 * @param src the source URI. If null or empty, nothing is included.
	 * You can specify the source URI with the query string and they
	 * will become a parameter that can be accessed by use
	 * of {@link Execution#getParameter} or javax.servlet.ServletRequest's getParameter.
	 * For example, if "/a.zul?b=c" is specified, you can access
	 * the parameter with ${param.b} in a.zul.
	 * @see #setDynamicProperty
	 */
	public void setSrc(String src) {
		if (src != null && src.length() == 0) src = null;

		if (!Objects.equals(_src, src)) {
			_src = src;
			fixMode();
			if (!_instantMode) invalidate();
		}
	}

	/** Returns the inclusion mode.
	 * There are three modes: auto, instant and defer.
	 * The behavior prior to 3.6.2 is the same as the defer mode.
	 * <p>It is recommended to use the auto mode if possible
	 * The reason to have <code>defer</code> as the default is to
	 * be backward compatible.
	 * <p>Default: defer.
	 * @since 3.6.2
	 */
	public String getMode() {
		return _mode;
	}
	/** Sets the inclusion mode.
	 * @param mode the inclusion mode: auto, instant or defer.
	 * @since 3.6.2
	 */
	public void setMode(String mode) throws WrongValueException {
		if (!_mode.equals(mode)) {
			if (!"auto".equals(mode) && !"instant".equals(mode)
			&& !"defer".equals(mode))
				throw new WrongValueException("Unknown mode: "+mode);
			if ((_localized || _progressing) && "instant".equals(mode))
				throw new UnsupportedOperationException("localized/progressing not allowed in instant mold");

			_mode = mode;
			fixMode();
		}
	}
	private void fixMode() {
		fixModeOnly();
		if (_instantMode && _afterComposed)
			afterCompose();
	}
	private void fixModeOnly() { //called by afterCompose
		boolean oldInstantMode = _instantMode;
		if ("auto".equals(_mode)) {
			if (_src != null && !_progressing && !_localized) {
				_instantMode = _src.endsWith(".zul") || _src.endsWith(".zhtml");
			} else
				_instantMode = false;
		} else
			_instantMode = "instant".equals(_mode);

		getChildren().clear();

		if (_instantMode != oldInstantMode)
			invalidate();
	}

	/** Returns whether the source depends on the current Locale.
	 * If true, it will search xxx_en_US.yyy, xxx_en.yyy and xxx.yyy
	 * for the proper content, where src is assumed to be xxx.yyy.
	 *
	 * <p>Default: false;
	 */
	public final boolean isLocalized() {
		return _localized;
	}
	/** Sets whether the source depends on the current Locale.
	 */
	public final void setLocalized(boolean localized) {
		if (_localized != localized) {
			if (localized && "instant".equals(_mode))
				throw new UnsupportedOperationException("localized not supported in instant mode yet");

			_localized = localized;
			if (_localized)
				fixMode();  //becomes defer mode if auto
			if (!_instantMode) //always instant mode but future we might support
				invalidate();
		}
	}

	//AfterCompose//
	public void afterCompose() {
		_afterComposed = true;
		fixModeOnly();
		if (_instantMode) {
			final Execution exec = getDesktop().getExecution();
			final Map old = setupDynams(exec);
			try {
				final int j = _src.indexOf('?');
				exec.createComponents(j >= 0 ? _src.substring(0, j): _src, this, null);
					//TODO: convert query string to arg
			} finally {
				restoreDynams(exec, old);
			}
		}
	}

	//DynamicPropertied//
	/** Returns whether a dynamic property is defined.
	 */
	public boolean hasDynamicProperty(String name) {
		return _dynams != null && _dynams.containsKey(name);
	}
	/** Returns the parameter associated with the specified name,
	 * or null if not found.
	 *
	 * @since 3.0.4
	 * @see #setDynamicProperty
	 */
	public Object getDynamicProperty(String name) {
		return _dynams != null ? _dynams.get(name): null;
	}
	/** Adds a dynamic property that will be passed to the included page
	 * via the request's attribute.
	 *
	 * <p>For example, if setDynamicProperty("abc", new Integer(4)) is called,
	 * then the included page can retrived the abc property
	 * by use of <code>${reqestScope.abc}</code>
	 *
	 * @since 3.0.4
	 */
	public void setDynamicProperty(String name, Object value) {
		if (_dynams == null)
			_dynams = new HashMap();
		_dynams.put(name, value);
	}
	/** Removes all dynamic properties.
	 * @since 3.6.4
	 */
	public void clearDynamicProperties() {
		_dynams = null;
	}

	//-- Component --//
	/** Invalidates this component.
	 * Notice that all children will be detached and the page will be
	 * reloaded (and new children will be created).
	 */
	public void invalidate() {
		if (_instantMode && _afterComposed) {
			getChildren().clear();
			afterCompose();
		} else
			super.invalidate();

		if (_progressStatus >= 2) _progressStatus = 0;
		checkProgressing();
	}
	/** Checks if _progressingg is defined.
	 */
	private void checkProgressing() {
		if(_progressing && _progressStatus == 0) {
			_progressStatus = 1;
			Clients.showBusy(Messages.get(MZul.PLEASE_WAIT), true);
			Events.echoEvent("onEchoInclude", this, null);
		}
	}

	/** Default: not childable.
	 */
	public boolean isChildable() {
		return _instantMode;
	}
	public void redraw(Writer out) throws IOException {
		if (_instantMode) {
			drawTagBegin(out);
			for (Component c = getFirstChild(); c != null; c = c.getNextSibling())
				c.redraw(out);
			drawTagEnd(out);
			return; //done
		}

		final UiEngine ueng =
			((WebAppCtrl)getDesktop().getWebApp()).getUiEngine();
		ueng.pushOwner(this);
		try {
			drawTagBegin(out);
			if (_progressStatus == 1) {
				_progressStatus = 2;
			} else if (_src != null && _src.length() > 0) {
				include(out);
			}
			drawTagEnd(out);
		} finally {
			ueng.popOwner();
		}
	}
	private void drawTagBegin(Writer out) throws IOException {
		out.write("<div id=\"");
		out.write(getUuid());
		out.write('"');
		out.write(getOuterAttrs());
		out.write(getInnerAttrs());
		out.write(">\n");
	}
	private void drawTagEnd(Writer out) throws IOException {
		out.write("\n</div>");
	}
	private void include(Writer out) throws IOException {
		final Desktop desktop = getDesktop();
		final Execution exec = desktop.getExecution();
		final String src = exec.toAbsoluteURI(_src, false);
		final Map old = setupDynams(exec);
		try {
			exec.include(out, src, null, 0);
		} catch (Throwable err) {
		//though DHtmlLayoutServlet handles exception, we still have to
		//handle it because src might not be ZUML
			final String errpg =
				desktop.getWebApp().getConfiguration().getErrorPage(
					desktop.getDeviceType(), err);
			if (errpg != null) {
				try {
					exec.setAttribute("javax.servlet.error.message", Exceptions.getMessage(err));
					exec.setAttribute("javax.servlet.error.exception", err);
					exec.setAttribute("javax.servlet.error.exception_type", err.getClass());
					exec.setAttribute("javax.servlet.error.status_code", new Integer(500));
					exec.include(out, errpg, null, 0);
					return; //done
				} catch (IOException ex) { //eat it (connection off)
				} catch (Throwable ex) {
					log.warning("Failed to load the error page: "+errpg, ex);
				}
			}

			final String msg = Messages.get(MZk.PAGE_FAILED,
				new Object[] {src, Exceptions.getMessage(err),
					Exceptions.formatStackTrace(null, err, null, 6)});
			final HashMap attrs = new HashMap();
			attrs.put(Attributes.ALERT_TYPE, "error");
			attrs.put(Attributes.ALERT, msg);
			exec.include(out,
				"~./html/alert.dsp", attrs, Execution.PASS_THRU_ATTR);
		} finally {
			restoreDynams(exec, old);
		}
	}
	private Map setupDynams(Execution exec) {
		if (_dynams == null || _dynams.isEmpty())
			return null;

		final Map old = new HashMap();
		for (Iterator it = _dynams.entrySet().iterator(); it.hasNext();) {
			final Map.Entry me = (Map.Entry)it.next();
			final String nm = (String)me.getKey();
			final Object val = me.getValue();

			old.put(nm, exec.getAttribute(nm));

			if (val != null) exec.setAttribute(nm, val);
			else exec.removeAttribute(nm);
		}
		return old;
	}
	private static void restoreDynams(Execution exec, Map old) {
		if (old != null)
			for (Iterator it = old.entrySet().iterator(); it.hasNext();) {
				final Map.Entry me = (Map.Entry)it.next();
				final String nm = (String)me.getKey();
				final Object val = me.getValue();

				if (val != null) exec.setAttribute(nm, val);
				else exec.removeAttribute(nm);
			}
	}
	private static String getDefaultMode() {
		if (_defMode == null)
			_defMode = Library.getProperty("org.zkoss.zul.include.mode", "defer");
		return _defMode;
	}
	private static String _defMode;
}
