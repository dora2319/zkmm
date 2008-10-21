/* create.js

{{IS_NOTE
	Purpose:
		
	Description:
		
	History:
		Sat Oct 18 19:24:38     2008, Created by tomyeh
}}IS_NOTE

Copyright (C) 2008 Potix Corporation. All Rights Reserved.

{{IS_RIGHT
}}IS_RIGHT
*/
/** Begins the creating of new page(s). */
function zknewbg() {
	zk.creating = true;
	zk.startProcessing(600);
}
/** Ends the creating of new page(s). */
function zknewe() {
	zk.creating = false;
	_zkws = []; //clean up if failed
	zkcurdt = null;
}

/** Used internally. */
function _zkbeg(w) {
	w.children = [];
	if (_zkws.length > 0)
		_zkws[0].children.add(w);
	_zkws.unshift(w);
}
/** Used internally. */
function _zkend() {
	var w = _zkws.shift();
	if (!_zkws.length) {
		_zkld(w); //OK to load JS before document.readyState complete

		_zkcrs.push([zkcurdt, w]);

		if (zk.creating) {
			if (zk.booted)
				_zkattach();
			else if (document.readyState) {
				var tid = setInterval(function(){
					if (/loaded|complete/.test(document.readyState)) {
						clearInterval(tid);
						_zkattach();
					}
				}, 50);
			} else //gecko
				setTimeout(_zkattach, 100);
				//don't count on DOMContentLoaded since the page might
				//be loaded by another ajax solution (i.e., portal)
				//Also, Bug 1619959: FF not fire it if in 2nd iframe
		} else
			_zkcreate();
	}
}

/** Begins the creation of a page generated by the server.
 *
 * @param contained if a page is not owned by another page, and
 * it doesn't cover the whole browser window (included by non-ZK tech)
 */
function zkpgbg(pgid, style, dtid, contained, updateURI) {
	if (dtid)
		zkdtbg(dtid, updateURI).pgid = pgid;

	_zkbeg({type: "#p", uuid: pgid,
		style: style ? style: "height:100%;width:100%",
		contained: contained});
}
/** Ends the creation of a page.
 */
zkpge = _zkend;

/** Begins the creation of a widget generated by the server.
 */
function zkbg(type, uuid, mold, props) {
	_zkbeg({type: type, uuid: uuid, mold: mold ? mold: "default",
		props: props});
}
/** Ends the creation of a widget. */
zke = _zkend;

/** Begins the creation of a desktop generated by the server.
 * This method is called only if zkpgbg is not called.
 * <p>Note: there is no zken().
 */
function zkdtbg(dtid, updateURI) {
	var dt = zk.Desktop.$(dtid);
	if (dt == null) dt = new zk.Desktop(dtid, updateURI);
	else if (updateURI) dt.updateURI = updateURI;
	zkcurdt = dt;
	return dt;
}

//Init Only//
/** Sets the version. */
function zkver() {
	var args = arguments, len = args.length;
	zk.version = args[0];
	zk.build = args[1];

	for (var j = 2; j < len; j += 2)
		zkPkg.version(args[j], args[j + 1]);
	return;
}

/** Sets the options. */
function zkopt(opts) {
	for (var nm in opts) {
		var val = opts[nm];
		switch (nm) {
		case "pd": zk.procDelay = val; break;
		case "td": zk.tipDelay =  val; break;
		case "rd": zk.resendDelay = val; break;
		case "dj": zk.debugJS = val; break;
		case "kd": zk.keepDesktop = val; break;
		case "pf": zk.pfmeter = val; break;
		}
	}
}

//Internal Use//
function _zkinit() {
	zk.booted = true;

	//TODO: listen document events
}
/** Used internally to redraw and attach. */
function _zkattach() {
	zkPkg.addAfterLoad(_zkattach0);
}
function _zkattach0() {
	if (!zk.booted)
		_zkinit();

	for (var inf; inf = _zkcrs.shift();) {
		var dt = inf[0], wginf = inf[1];

		var wgt = _zkcreate1(null, wginf);
		zkDom.outerHTML(zkDom.$(wgt.uuid), wgt.redraw());
	}

	zk.endProcessing();
}
/** Used internally to create the widget tree based on _zkcrs. */
function _zkcreate() {
	zkPkg.addAfterLoad(_zkcreate0);
}
function _zkcreate0() {
	for (var inf; inf = _zkcrs.shift();) {
		_zkcreate1(null, inf[1]); //TODO : add result to a list
	}
}
/** Used internally to create the widget tree. */
function _zkcreate1(parent, wginf) {
	var wgt;
	if (wginf.type == "#p") {
		wgt = new zk.Page(wginf.uuid, wginf.contained);
		if (wginf.style) wgt.style = wginf.style;
		if (parent) parent.appendChild(wgt);
	} else {
		var cls = zk.$import(wginf.type),
			uuid = wginf.uuid, props = wginf.props,
			wgt = new cls(uuid, wginf.mold),
			embedAs = cls.embedAs;
		wgt.inServer = true;
		if (parent) parent.appendChild(wgt);

		//embedAs means value from element's text
		if (embedAs && !props[embedAs]) {
			var embed = zkDom.$(uuid);
			if (embed)
				props[embedAs] = embed.innerHTML;
		}

		for (var p in props) {
			var m = wgt['set' + p.charAt(0).toUpperCase() + p.substring(1)];
			if (m) m(props[p]);
			else wgt[p] = props[p];
		}
	}

	for (var j = 0, childs = wginf.children, len = childs.length;
	j < len; ++j)
		_zkcreate1(wgt, childs[j]);

	return wgt;
}
/** Used internally to load package of the specified widget/page. */
function _zkld(w) {
	var type = w.type; j = type.lastIndexOf('.');
	if (j >= 0)
		zkPkg.load(type.substring(0, j), zkcurdt);
	for (var children = w.children, len = children.length, j = 0; j < len;++j)
		_zkld(children[j]);
}

/** Used internally. */
var _zkws = [], zkcurdt, _zkcrs = []; //used to load widget
