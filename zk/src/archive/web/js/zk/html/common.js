/* common.js

{{IS_NOTE
	$Id: common.js,v 1.39 2006/05/22 02:26:42 tomyeh Exp $
	Purpose:
		Common utiltiies.
	Description:
		
	History:
		Fri Jun 10 18:16:11     2005, Created by tomyeh@potix.com
}}IS_NOTE

Copyright (C) 2005 Potix Corporation. All Rights Reserved.

{{IS_RIGHT
	This program is distributed under GPL Version 2.0 in the hope that
	it will be useful, but WITHOUT ANY WARRANTY.
}}IS_RIGHT
*/

// Standard //
String.prototype.startsWith = function (prefix) {
	return this.substring(0,prefix.length) == prefix;
};
String.prototype.endsWith = function (suffix) {
	return this.substring(this.length-suffix.length) == suffix;
};
String.prototype.trim = function () {
	var j = 0, k = this.length - 1;
	while (j < this.length && this.charAt(j) <= ' ')
		++j;
	while (k >= j && this.charAt(k) <= ' ')
		--k;
	return j > k ? "": this.substring(j, k + 1);
};
String.prototype.skipWhitespaces = function (j) {
	for (;j < this.length; ++j) {
		var cc = this.charAt(j);
		if (cc != ' ' && cc != '\t' && cc != '\n' && cc != '\r')
			break;
	}
	return j;
};
String.prototype.nextWhitespace = function (j) {
	for (;j < this.length; ++j) {
		var cc = this.charAt(j);
		if (cc == ' ' || cc == '\t' || cc == '\n' || cc == '\r')
			break;
	}
	return j;
};
String.prototype.skipWhitespacesBackward = function (j) {
	for (;j >= 0; --j) {
		var cc = this.charAt(j);
		if (cc != ' ' && cc != '\t' && cc != '\n' && cc != '\r')
			break;
	}
	return j;
};

/** Removes the specified object from the array if any.
 * Returns false if not found.
 */
Array.prototype.remove = function (o) {
	for (var j = 0; j < this.length; ++j) {
		if (o == this[j]) {
			this.splice(j, 1);
			return true;
		}
	}
	return false;
};
/** Returns whether the array contains the specified object.
 */
Array.prototype.contains = function (o) {
	for (var j = 0; j < this.length; ++j) {
		if (o == this[j])
			return true;
	}
	return false;
};

//
// More zk utilities (defined also in boot.js) //

/** Center the specified element. */
zk.center = function (el) {
	var elwd = el.offsetWidth;
	var elhgh = el.offsetHeight;

	var height = zk.innerHeight();
	var width = zk.innerWidth();
	var top = zk.innerY();
	var left = zk.innerX();

	var ofs = zk.toParentOffset(el,
		left + (width - elwd) / 2, top + (height - elhgh) / 2);
	el.style.left = ofs[0] + "px"; el.style.top =  ofs[1] + "px";
};
/** Position a component being releted to another. */
zk.position = function (el, ref, type) {
	var refofs = Position.cumulativeOffset(ref);
	var x, y;
	if (type == "end_before") { //el's upper-left = ref's upper-right
		x = refofs[0] + ref.offsetWidth;
		y = refofs[1];

		if (zk.agtIe) {
			var diff = parseInt(zk.getCurrentStyle(ref, "margin-top")||"0", 10);
			if (!isNaN(diff)) y += diff;
			diff = parseInt(zk.getCurrentStyle(ref, "margin-right")||"0", 10);
			if (!isNaN(diff)) x += diff;
		}
	} else { //after-start: el's upper-left = ref's lower-left
		x = refofs[0];
		var max = zk.innerWidth() - el.offsetWidth;
		if (x > max) x = max;
		y = refofs[1] + ref.offsetHeight;

		if (zk.agtIe) {
			var diff = parseInt(zk.getCurrentStyle(ref, "margin-bottom")||"0", 10);
			if (!isNaN(diff)) y += diff;
			diff = parseInt(zk.getCurrentStyle(ref, "margin-left")||"0", 10);
			if (!isNaN(diff)) x += diff;
		}
	}

	refofs = zk.toParentOffset(el, x, y);
	el.style.left = refofs[0] + "px"; el.style.top = refofs[1] + "px";
};

/** Converts to coordination related to the containing element.
 * This is useful if you need to specify el.style.left or top.
 */
zk.toParentOffset = function (el, x, y) {
      var p = el.offsetParent;
      if (p) {
          var refofs = Position.cumulativeOffset(p);
          x -= refofs[0]; y -= refofs[1];
      }
      return [x, y];
};
/** Returns the style's coordination in [integer, integer].
 * Note: it ignores the unit and assumes px (so pt or others will be wrong)
 */
zk.getStyleOffset = function (el) {
	return [parseInt(el.style.left || '0'), parseInt(el.style.top || '0')];
};

/** Whether el1 and el2 are overlapped. */
zk.isOverlapped = function (el1, el2) {
	return zk.isOffsetOverlapped(
		Position.cumulativeOffset(el1), [el1.offsetWidth, el1.offsetHeight],
		Position.cumulativeOffset(el2), [el2.offsetWidth, el2.offsetHeight]);
};
/** Whether ofs1/dim1 is overlapped with ofs2/dim2. */
zk.isOffsetOverlapped = function (ofs1, dim1, ofs2, dim2) {
	var o1x1 = ofs1[0], o1x2 = dim1[0] + o1x1,
		o1y1 = ofs1[1], o1y2 = dim1[1] + o1y1;
	var o2x1 = ofs2[0], o2x2 = dim2[0] + o2x1,
		o2y1 = ofs2[1], o2y2 = dim2[1] + o2y1;
	return o2x1 <= o1x2 && o2x2 >= o1x1 && o2y1 <= o1y2 && o2y2 >= o1y1;
};

/** Whether an element is visible. */
zk.isVisible = function (el) {
	return el && el.style && el.style.display != "none";
};
/** Whether an element is really visible. */
zk.isRealVisible = function (e) {
	if (!e) return false;
	do {
		if (e.style && e.style.display == "none") return false;
		//note: document is the top parent and has NO style
	} while (e = e.parentNode);
	return true;
};

/** Focus the specified element and any of its child. */
zk.focusDown = function (el) {
	return zk._focusDown(el, new Array("INPUT", "SELECT", "BUTTON"), true)
		|| zk._focusDown(el, new Array("A"), false);
};
/** checkA whether to check the A tag specially (i.e., focus if one ancestor
 * has zk_type.
 */
zk._focusDown = function (el, match, checkA) {
	if (!el) return false;
	if (el.focus) {
		var tn = zk.tagName(el);
		if (match.contains(tn)) {
			try {el.focus();} catch (e) {}
			//IE throws exception when focus in some cases
			return true;
		}
		if (checkA && tn == "A") {
			for (var n = el; (n = n.parentNode) != null;) {
				if (n.getAttribute("zk_type")) {
					try {el.focus();} catch (e) {}
					//IE throws exception when focus in some cases
					return true;
				}
			}
		}
	}
	for (el = el.firstChild; el; el = el.nextSibling) {
		if (zk._focusDown(el, match))
			return true;
	}
	return false;
};
/** Focus the element with the specified ID and do it timeout later. */
zk.focusDownById = function (id, timeout) {
	var script = "if (!zk.focusDown($('"+id+"'))) window.focus()";
	zk._doTwice(script, timeout);
};
/** Focus the element without looking down, and do it timeout later. */
zk.focusById = function (id, timeout) {
	var script = "zk._focus($('"+id+"'))";
	zk._doTwice(script, timeout);
};
zk._focus = function (cmp) {
	if (cmp && cmp.focus) try {cmp.focus();} catch (e) {}
		//IE throws exception when focus in some cases
};
/** Select the text of the element, and do it timeout later. */
zk.selectById = function (id, timeout) {
	var script = "zk._select($('"+id+"'))";
	zk._doTwice(script, timeout);
};
zk._select = function (cmp) {
	if (cmp && cmp.select) try {cmp.select();} catch (e) {}
		//IE throws exception when focus() in some cases
};
zk._doTwice = function (script, timeout) {
	if (!timeout) timeout = 0;
	setTimeout(script, timeout);
	setTimeout(script, timeout);
		//Workaround for an IE bug: we have to set focus twice since
		//the first one might fail (even we prolong the timeout to 1 sec)
};

/** Inserts an unparsed HTML immediately before the specified element.
 * @param el the sibling before which to insert
 */
zk.insertHTMLBefore = function (el, html) {
	if (zk.agtIe) {
		switch (zk.tagName(el)) { //exclude TABLE
		case "TD": case "TH": case "TR": case "CAPTION":
		case "TBODY": case "THEAD": case "TFOOT":
			var n = document.createElement(zk._agtIeTagOfHtml(html));
			el.parentNode.insertBefore(n, el);
			zk._agtIeReplaceOuterHTML(n, html);
			return;
		}
	}
	el.insertAdjacentHTML('beforeBegin', html);
};
/** Inserts an unparsed HTML immediately before the ending element.
 */
zk.insertHTMLBeforeEnd = function (el, html) {
	if (zk.agtIe) {
		switch (zk.tagName(el)) {
		case "TABLE": case "TR":
		case "TBODY": case "THEAD": case "TFOOT":
		/*case "TH": case "TD": case "CAPTION":*/ //no need to handle them
			var n = document.createElement(zk._agtIeTagOfHtml(html));
			el.appendChild(n);
			zk._agtIeReplaceOuterHTML(n, html);
			return;
		}
	}
	el.insertAdjacentHTML("beforeEnd", html);
};
/** Inserts an unparsed HTML immediately after the specified element.
 * @param el the sibling after which to insert
 */
zk.insertHTMLAfter = function (el, html) {
	if (zk.agtIe) {
		switch (zk.tagName(el)) { //exclude TABLE
		case "TD": case "TH": case "TR": case "CAPTION":
		case "TBODY": case "THEAD": case "TFOOT":
			var sib = el.nextSibling;
			if (sib != null) {
				zk.insertHTMLBefore(sib, html);
			} else {
				var n = document.createElement(zk._agtIeTagOfHtml(html));
				el.parentNode.appendChild(n);
				zk._agtIeReplaceOuterHTML(n, html);
			}
			return;
		}
	}
	el.insertAdjacentHTML('afterEnd', html);
};

/** Sets the inner HTML.
 */
zk.setInnerHTML = function (el, html) {
	if (zk.agtIe) {
		zk._agtIeReplaceInnerHTML(el, html);
	} else {
		el.innerHTML = html;
	}
};
/** Sets the outer HTML.
 */
zk.setOuterHTML = function (el, html) {
	if (zk.agtIe) {
		var tn = zk.tagName(el);
		if (tn == "TD" || tn == "TH" || tn == "TABLE" || tn == "TR"
		|| tn == "CAPTION" || tn == "TBODY" || tn == "THEAD"
		|| tn == "TFOOT") {
			zk._agtIeReplaceOuterHTML(el, html);
			return;
		}
	}
	el.outerHTML = html;
};

/** Returns the next sibling with the specified tag name, or null if not found.
 */
zk.nextSibling = function (el, tagName) {
	while (el && (el = el.nextSibling) != null && zk.tagName(el) != tagName)
		;
	return el;
};
/** Returns the next sibling with the specified tag name, or null if not found.
 */
zk.previousSibling = function (el, tagName) {
	while (el && (el = el.previousSibling) != null && zk.tagName(el) != tagName)
		;
	return el;
};
/** Returns the parent with the specified tag name, or null if not found.
 */
zk.parentNode = function (el, tagName) {
	while (el && (el = el.parentNode) != null && zk.tagName(el) != tagName)
		;
	return el;
};

/** Returns the first child of the specified node. */
zk.firstChild = function (el, tagName, descendant) {
	for (var n = el.firstChild; n; n = n.nextSibling)
		if (zk.tagName(n) == tagName)
			return n;

	if (descendant) {
		for (var n = el.firstChild; n; n = n.nextSibling) {
			var chd = zk.firstChild(n, tagName, descendant);
			if (chd)
				return chd;
		}
	}
	return null;
}

/** Returns whether a node is an ancestor of another (including itself). */
zk.isAncestor = function(p, c) {
	for (; c; c = c.parentNode)
		if (p == c)
			return true;
	return false;
}

/** Appends an unparsed HTML immediately after the last child.
 * @param el the parent
 */
//zk.appendHTMLChild = function (el, html) {
//	el.insertAdjacentHTML('beforeEnd', html);
//};
if (zk.agtIe) {
	/** Returns the enclosing tag for the specified HTML codes.
	 */
	zk._agtIeTagOfHtml = function (html) {
		var j = html.indexOf('>'), k = html.lastIndexOf('<');
		if (j < 0 || k < 0) {
			alert("Unknown tag: "+html);
			return "";
		}
		var head = html.substring(0, j);
		j = head.indexOf('<') + 1;
		j = head.skipWhitespaces(j);
		k = head.nextWhitespace(j);
		return head.substring(j, k).toUpperCase();
	}
	/** Replace HTML for TR, TD and others; the same as outerHTML, used
	 * since IE don't support non-SPAN/DIV well.
	 */
	zk._agtIeReplaceOuterHTML = function (el, html) { //patch for IE
		var j = html.indexOf('>');
		if (j < 0) {
			alert("Unsupported replace: "+html);
			return;
		}

		var head, inner, k;
		for (k = j; --k >= 0;) {
			var cc = html.charAt(k);
			if (cc == ' ' || cc == '\t' || cc == '\n') continue;
			if (cc == '/') {
				head = html.substring(0, k);
				inner = "";
			}
			break;
		}
		if (!head) {
			head = html.substring(0, j);
			k = html.lastIndexOf('<');
			inner = k > j ? html.substring(j + 1, k): "";
		}

		//replace attributes
		//Potential bug: we don't remove attributes not found in html
		//It is enough for now but we might need to do it in FUTURE
		j = head.indexOf('<') + 1;
		j = head.skipWhitespaces(j);
		k = head.nextWhitespace(j);
		var tag = head.substring(j, k).toUpperCase();
		if (zk.tagName(el) != tag) {
			alert("Unsupported replace: different tags: old="+el.tagName+", new="+tag);
			return;
		}

		for (;;) {
			j = k;
			j = head.skipWhitespaces(j);
			if (j >= head.length) break; //done
			k = head.indexOf('=', j);
			if (k < 0) {
				alert("Unsupported: attribute must have a value:\n"+head)
				return;
			}
			var attr = head.substring(j, head.skipWhitespacesBackward(k))
				.toLowerCase();
			var val;
			j = head.skipWhitespaces(k + 1);
			if (head.charAt(j) == '"') {
				k = head.indexOf('"', ++j);
				if (k < 0) k = head.length;
				val = head.substring(j, k);
				++k;
			} else {
				k = head.nextWhitespace(j);
				val = head.substring(j, k);
			}

			switch (attr) {
			case "id": el.id = val; break;
			case "class": el.className = val; break;
			case "style": zk.setStyle(el, val); break;
			case "onclick":
			case "ondblclick":
			case "onkeydown":
			case "onkeypress":
			case "onkeyup":
			case "onmousedown":
			case "onmousemove":
			case "onmouseout":
			case "onmouseover":
			case "onmouseup":
				el[attr] = new Function(val); break;
			case "colspan": el.colSpan = val; break; //IE bug
			case "rowspan": el.rowSpan = val; break; //IE bug
			case "cellpadding": el.cellPadding = val; break; //IE bug
			case "cellspacing": el.cellSpacing = val; break; //IE bug
			case "valign": el.vAlign = val; break; //IE bug
			default: el.setAttribute(attr, val);
			}
		}

		if (inner)
			zk._agtIeReplaceInnerHTML(el, inner);
	};
	/** Replace HTML for TR, TD and others; the same as outerHTML, used
	 * since IE don't support non-SPAN/DIV well.
	 */
	zk._agtIeReplaceInnerHTML = function (el, html) { //patch for IE
		//replace inner
		var tn = zk.tagName(el);
		if (tn == "TR" || tn == "TABLE" || tn == "TBODY" || tn == "THEAD"
		|| tn == "TFOOT") { //ignore TD/TH/CAPTION
			while (el.firstChild)
				el.removeChild(el.firstChild);

			if (tn == "TABLE") {
				var tagInfo = zk._agtIeNextTag(html, 0);
				if (tagInfo && tagInfo.tagName == "TR") {
					var n = document.createElement("TBODY");
					el.appendChild(n);
					el = n;
				}
			}

			for (var j = 0, depth = 0; j < html.length;) {
				var tagInfo = zk._agtIeNextTag(html, j);
				if (!tagInfo) return;

				var tagnm = tagInfo.tagName;
				var n = document.createElement(tagnm);
				el.appendChild(n);

				var k = html.indexOf('>', tagInfo.index);
				for (var depth = 0; k >= 0;) {
					tagInfo = zk._agtIeNextTag(html, k + 1);
					if (!tagInfo) break;
					k = html.indexOf('>', tagInfo.index);
					if (tagnm == tagInfo.tagName) {
						++depth;
					} else if ("/" + tagnm == tagInfo.tagName) {
						if (--depth < 0)
							break;
					}
				}

				if (k < 0) k = html.length;
				else ++k;
				zk._agtIeReplaceOuterHTML(n, html.substring(j, k));
				j = k;
			}
		} else {
			//Note: if TD/TH/CAPTION, we cannot use innerText
			if (tn == "TD" || tn == "TH" || tn == "CAPTION"
			|| html.indexOf('<') >= 0) {
				el.innerHTML = html;
			} else {
			//20060331: IE bug: innerHTML ignores \n (even white-space:pre)
			//To workaround, we have to use innerText but innerText
			//won't handle &xx;, so...
				var j = html.indexOf('&');
				if (j >= 0) {
					var cvt = "";
					for (var l = 0;;) {
						var k = html.indexOf(';', j + 1);
						if (k < 0) {
							cvt += html.substring(l);
							break; //done
						} else {
							cvt += html.substring(l, j);
							var s = html.substring(j + 1, k);
							switch (s) {
							case "amp": cvt += '&'; break;
							case "lt": cvt += '<'; break;
							case "gt": cvt += '>'; break;
							case "quot": cvt += '"'; break;
							case "apos": cvt += "'"; break;
							default:
								if (s.length && s.charAt(0) == '#') {
									cvt += String.fromCharCode(parseInt(s.substring(1),10));
								} else {
									cvt += html.substring(j, k + 1);
								}
							}

							j = html.indexOf('&', l = k + 1);
							if (j < 0) {
								cvt += html.substring(l);
								break; //done
							}
						}
					}
					html = cvt;
				}
				el.innerText = html;
			}
		}
	};
	/** Next tag info. */
	zk._agtIeNextTag = function (html, j) {
		var k = html.indexOf('<', j);
		if (k < 0) return null;

		var l = html.skipWhitespaces(k + 1);
		var tagnm = "";
		if (html.charAt(l) == '/') {
			tagnm = "/";
			l = html.skipWhitespaces(l + 1);
		}

		for (;; ++l) {
			if (l >= html.length) return null; //ignore it
			var cc = html.charAt(l);
			if ((cc < 'a' || cc > 'z') && (cc < 'A' || cc > 'Z'))
				break;
			tagnm += cc;
		}
		return {tagName: tagnm.toUpperCase(), index: l};
	};
}

/** Returns the element's value (by catenate all CDATA and text).
 */
zk.getElementValue = function (el) {
	var txt = ""
	for (el = el.firstChild; el; el = el.nextSibling)
		if (el.data) txt += el.data;
	return txt;
};

/** Extends javascript for Non-IE
 */
if (!zk.agtIe) {
	//1. outerHTML
	var setOuterHTML = function (html) {
		var range = this.ownerDocument.createRange();
		range.setStartBefore(this);
		var df = range.createContextualFragment(html);
		this.parentNode.replaceChild(df, this);
	};

	if(window.HTMLElement)
		HTMLElement.prototype.__defineSetter__("outerHTML", setOuterHTML);
	else
		alert(mesg.UNSUPPORTED_BROWSER+zk.agent);

	//2. insertAdjacentHTML
	HTMLElement.prototype.insertAdjacentHTML = function (sWhere, sHTML) {
		var df;   // : DocumentFragment
		var r = this.ownerDocument.createRange();

		switch (String(sWhere).toLowerCase()) {  // convert to string and unify case
		case "beforebegin":
			r.setStartBefore(this);
			df = r.createContextualFragment(sHTML);
			this.parentNode.insertBefore(df, this);
			break;

		case "afterbegin":
			r.selectNodeContents(this);
			r.collapse(true);
			df = r.createContextualFragment(sHTML);
			this.insertBefore(df, this.firstChild);
			break;

		case "beforeend":
			r.selectNodeContents(this);
			r.collapse(false);
			df = r.createContextualFragment(sHTML);
			this.appendChild(df);
			break;

		case "afterend":
			r.setStartAfter(this);
			df = r.createContextualFragment(sHTML);
			if (this.nextSibling)
				this.parentNode.insertBefore(df, this.nextSibling);
			else
				this.parentNode.appendChild(df);
			break;
		}
	};
};

//-- Image utilities --//
/* Rename by changing the type (after -)
//name: the original name
//type: the type to rename to: open or closed
//todo: support _zh_TW
*/
zk.rename = function (name, type) {
	var j = name.lastIndexOf('.');
	var k = name.lastIndexOf('-');
	var m = name.lastIndexOf('/');
	var ext = j <= m ? "": name.substring(j);
	var pref = k <= m ? j <= m ? name: name.substring(0, j): name.substring(0, k);
	if (type) type = "-" + type;
	return pref + type + ext;
};

//-- special routines --//
if (!zk.activeTagnames) {
	zk.activeTagnames =
		new Array("A","BUTTON","TEXTAREA","INPUT","SELECT","IFRAME","APPLET");
	zk._disTags = new Array(); //A list of {element: xx, what: xx}

	if (zk.agtIe) {
		zk._hidCvred = new Array(); //A list of {element: xx, visibility: xx}
		zk.coveredTagnames = new Array("SELECT"/*,"IFRAME","APPLET"*/);
			//though IFRAME might include SELECT (but prefer not to surprise user)
	} else {
		zk.coveredTagnames = new Array();
	}
}

/** Disables all active tags. */
zk.disableAll = function (parent) {
	for (var j = 0; j < zk.activeTagnames.length; j++) {
		var els = document.getElementsByTagName(zk.activeTagnames[j]);
		l_els:
		for (var k = 0 ; k < els.length; k++) {
			var el = els[k];
			if (zk.isAncestor(parent, el))
				continue;
			for(var m = 0; m < zk._disTags.length; ++m) {
				var info = zk._disTags[m];
				if (info.element == el)
					continue l_els;
			}

			if (zk._disTags.contains(el))
				continue;

			var what;
			var tn = zk.tagName(el);
			if (tn == "IFRAME" || tn == "APPLET" || (zk.agtIe && tn == "SELECT")) {
	//Note: we don't check isOverlapped because mask always covers it
				what = el.style.visibility;
				el.style.visibility = "hidden";
			} else if (!zk.agtIe && tn == "A") {
	//Firefox doesn't support the disable of A
				what = "h:" + zkau.getStamp(el, "href") + ":" + el.href;
				el.href = "";
			} else {
				what = "d:" + zkau.getStamp(el, "disabled") + ":" + el.disabled;
				el.disabled = true;
			}
			zk._disTags.push({element: el, what: what});
		}
	}
};
/** Restores tags being disabled by previous disableAll. If el is not null,
 * only el's children are enabled
 */
zk.restoreDisabled = function (n) {
	var skipped = new Array();
	for (;;) {
		var info = zk._disTags.shift();
		if (!info) break;

		var el = info.element;
		if (el && el.tagName) { //still exists
			if (n && !zk.isAncestor(n, el)) { //not processed yet
				skipped.push(info);
				continue;
			}
			var what = info.what;
			if (what.startsWith("d:")) {
				var j = what.indexOf(':', 2);
				if (what.substring(2, j) == zkau.getStamp(el, "disabled"))
					el.disabled = what.substring(j + 1) == "true";
			} else if (what.startsWith("h:")) {
				var j = what.indexOf(':', 2);
				if (what.substring(2, j) == zkau.getStamp(el, "href"))
					el.href = what.substring(j + 1);
			} else 
				el.style.visibility = what;
		}
	}
	zk._disTags = skipped;
};
/** Hide select, iframe and applet if it is covered by any of ary
 * and not belonging to any of ary.
 * If ary is empty, it restores what have been hidden by last invocation
 * to this method.
 */
zk.hideCovered = function (ary) {
	if (!zk.agtIe) return; //nothing to do

	if (!ary || ary.length == 0) {
		for (;;) {
			var info = zk._hidCvred.shift();
			if (!info) break;

			if (info.element.style)
				info.element.style.visibility = info.visibility;
		}
		return;
	}

	for (var j = 0; j < zk.coveredTagnames.length; ++j) {
		var els = document.getElementsByTagName(zk.coveredTagnames[j]);
		loop_els:
		for (var k = 0 ; k < els.length; k++) {
			var el = els[k];
			if (!zk.isRealVisible(el)) continue;

			for (var m = 0; m < ary.length; ++m) {
				if (zk.isAncestor(ary[m], el))
					continue loop_els;
			}

			var overlapped = false;
			for (var m = 0; m < ary.length; ++m) {
				if (zk.isOverlapped(ary[m], el)) {
					overlapped = true;
					break;
				}
			}

			if (overlapped) {
				for (var m = 0; m < zk._hidCvred.length; ++m) {
					if (el == zk._hidCvred[m].element)
						continue loop_els;
				}
				zk._hidCvred
					.push({element: el, visibility: el.style.visibility});
				el.style.visibility = "hidden";
			} else {
				for (var m = 0; m < zk._hidCvred.length; ++m) {
					if (el == zk._hidCvred[m].element) {
						el.style.visibility = zk._hidCvred[m].visibility;
						zk._hidCvred.splice(m, 1);
						break;
					}
				}
			}
		}
	}
};

/** Retrieve a member by use of a.b.c */
zk.resolve = function (fullnm) {
	for (var j = 0, v = window;;) {
		var k = fullnm.indexOf('.', j);
		var nm = k >= 0 ? fullnm.substring(j, k): fullnm.substring(j);
		v = v[nm];
		if (k < 0 || !v) return v;
		j = k + 1;
	}
};

/** Sets the style. */
zk.setStyle = function (el, style) {
	for (var j = 0, k = 0; k >=0; j = k + 1) {
		k = style.indexOf(';', j);
		var s = k >= 0 ? style.substring(j, k): style.substring(j);
		var l = s.indexOf(':');
		var nm, val;
		if (l < 0) {
			nm = s.trim(); val = "";
		} else {
			nm = s.substring(j, l).trim();
			val = s.substring(l + 1).trim();
		}
		if (nm) el.style[zk.toJSStyleName(nm)] = val;
	}
};

/** Returns the text-relevant style (same as HTMLs.getTextRelevantStyle).
 * @param incwd whether to include width
 * @param inchgh whether to include height
 */
zk.getTextStyle = function (style, incwd, inchgh) {
	var s = "";
	for (var j = 0, l = 0, k; l >= 0; j = l + 1) {
		k = style.indexOf(':', j);
		l = k >= 0 ? style.indexOf(';', k + 1): -1;

		var nm = k >= 0 ? style.substring(j, k): style.substring(j);
		nm = nm.trim();

		if (nm.startsWith("font")  || nm.startsWith("text")
		|| zk._txtstyles.contains(nm)
		|| (incwd && nm == "width") || (inchgh && nm == "height"))
			s += l >= 0 ? style.substring(j, l + 1): style.substring(j);
	}
	return s;
};
if (!zk._txtstyles)
	zk._txtstyles = ["color", "background-color", "background",
		"white-space"];

/** Returns the current style. */
zk.getCurrentStyle = function (el, prop) {
	return document.defaultView && document.defaultView.getComputedStyle ?
			document.defaultView.getComputedStyle(el,null).getPropertyValue(prop):
		el.currentStyle ? el.currentStyle[zk.toJSStyleName(prop)]:
			el.style[zk.toJSStyleName(prop)];
};
/** Converts a style name to JavaScript name. */
zk.toJSStyleName = function (nm) {
	var j = nm.indexOf('-');
	if (j < 0) return nm;
	if (j >= nm.length - 1) return nm.substring(0, j);
	return nm.substring(0, j) + nm.substring(j+1, j+2).toUpperCase()
		+ nm.substring(j + 2);
};

/** Backup a style of the specified name.
 * The second call to backupStyle is ignored until zk.restoreStyle is called.
 * Usually used with onmouseover.
 */
zk.backupStyle = function (el, nm) {
	var bknm = "zk_bk" + nm;
	if (!el.getAttribute(bknm))
		el.setAttribute(bknm, el.style[nm] || "_zk_none_");
};
/** Restore the style of the specified name.
 * Usually used with onover.
 */
zk.restoreStyle = function (el, nm) {
	var bknm = "zk_bk" + nm;
	var val = el.getAttribute(bknm);
	if (val) {
		el.removeAttribute(bknm);
		el.style[nm] = val == "_zk_none_" ? "": val;
	}
};

/** Scroll inner into visible, assuming outer has a scrollbar. */
zk.scrollIntoView = function (outer, inner) {
	if (outer && inner) {
		var padding = zk.getCurrentStyle(inner, "padding-top");
		padding = padding ? parseInt(padding, 10): 0;
		var limit = inner.offsetTop - padding;
		if (limit < outer.scrollTop) {
			outer.scrollTop = limit;
		} else {
			limit = 3 + inner.offsetTop + inner.offsetHeight
				- outer.scrollTop - outer.clientHeight;
			if (limit > 0) outer.scrollTop += limit;
		}
	}
};

/** Go to the specified uri.
 * @param overwrite whether to overwrite the history
 * @param target the target frame (ignored if overwrite is true
 */
zk.go = function (url, overwrite, target) {
	if (overwrite) {
		document.location.replace(url);
	} else {
		//we have to process query string because browser won't do it
		//even if we use insertHTMLBeforeEnd("<form...")
		var frm = document.createElement("FORM");
		document.body.appendChild(frm);
		var j = url.indexOf('?');
		if (j > 0) {
			var qs = url.substring(j + 1);
			url = url.substring(0, j);
			zk.queryToHiddens(frm, qs);
		}
		frm.name = "go";
		frm.action = url;
		frm.method = "GET";
		if (target) frm.target = target;
		if (url && !zk.isNewWindow(url, target)) zk.progress();
		frm.submit();
	}
};
/** Tests whether a new window will be opened.
 */
zk.isNewWindow = function (url, target) {
	return url.startsWith("mailto:") || url.startsWith("javascript:")
		|| (target && target != "_self");
};

/* Convert query string (a=b&c=d) to hiddens of the specified form.
 */
zk.queryToHiddens = function (frm, qs) {
	for(var j = 0;;) {
		var k = qs.indexOf('=', j);
		var l = qs.indexOf('&', j);

		var inp = document.createElement("INPUT");
		inp.type = "hidden";
		frm.appendChild(inp);

		if (k < 0 || (k > l && l >= 0)) { //no value part
			inp.name = l >= 0 ? qs.substring(j, l): qs.substring(j);
			inp.value = "";
		} else {
			inp.name = qs.substring(j, k);
			inp.value = l >= 0 ? qs.substring(k + 1, l): qs.substring(k + 1);
		}

		if (l < 0) return; //done
		j = l + 1;
	}
}

/** Creates a hidden frame if it is not created yet. */
zk.newFrame = function (name) {
	var frm = $(name);
	if (frm) return frm;

	zk.insertHTMLBeforeEnd(document.body,
		'<iframe id="'+name+'" name="'+name+'" style="display:none"></iframe>');
	return $(name);
};

/** Copies the width of each cell from one row to another.
 * @param srcrows all rows from the source table. Don't pass just one row
 * because a row might not have all cells.
 * @param times how many times to copy the cell width.
 * It is useful since browser might re-adjust source's cell width.
 * after dst is changed
 */
zk.cpCellWidth = function (dst, srcrows, times) {
	if (dst == null || srcrows == null || !srcrows.length
	|| !dst.cells || !dst.cells.length)
		return;

	var max = 0, src;
	for (var j = 0; j < srcrows.length; ++j) {
		var l = srcrows[j].cells.length;
		if (l > max) {
			max = l;
			src = srcrows[j];
			if (max >= dst.cells.length) {
				max = dst.cells.length;
				break; //done
			}
		}
	}
	if (!src) return; //no visible cells

	for (var j = 0; j < max; ++j) {
		var d = dst.cells[j], s = src.cells[j];
		d.style.width = s.offsetWidth + "px";
		var v = s.offsetWidth - d.offsetWidth;
		if (v != 0) {
			v += s.offsetWidth;
			if (v < 0) v = 0;
			d.style.width = v + "px";
		}
	}
	if (times > 0)
		setTimeout(function() {zk.cpCellWidth(dst, srcrows, times - 1);}, 100);
};

//Number//
/** digits specifies at least the number of digits must be ouput. */
zk.formatFixed = function (val, digits) {
	var s = "" + val;
	for (var j = digits - s.length; --j >= 0;)
		s = "0" + s;
	return s;
};

//Date//
/** Parses a string into a Date object.
 * @param strict whether not to lenient
 */
zk.parseDate = function (txt, fmt, strict) {
	if (!fmt) fmt = "yyyy/MM/dd";
	var val = new Date();
	var y = val.getFullYear(), m = val.getMonth(), d = val.getDate();

	var ts = txt.split(/\W+/);
	for (var i = 0, j = 0; j < fmt.length; ++j) {
		var cc = fmt.charAt(j);
		if (cc == 'y' || cc == 'M' || cc == 'd' || cc == 'E') {
			var len = 1;
			for (var k = j; ++k < fmt.length; ++len)
				if (fmt.charAt(k) != cc)
					break;

			var nosep; //no separator
			if (k < fmt.length) {
				var c2 = fmt.charAt(k);
				nosep = c2 == 'y' || c2 == 'M' || c2 == 'd' || c2 == 'E';
			}

			var token = ts[i++];
			switch (cc) {
			case 'y':
				if (nosep) {
					if (len <= 3) len = 2;
					if (token.length > len) {
						ts[--i] = token.substring(len);
						token = token.substring(0, len);
					}
				}
				y = parseInt(token, 10);
				if (isNaN(y)) return null; //failed
				if (y < 100) y += y > 29 ? 1900 : 2000;
				break;
			case 'M':
				if (len <= 2) {
					if (nosep && token.length > 2) {
						ts[--i] = token.substring(2);
						token = token.substring(0, 2);
					}
					m = parseInt(token, 10) - 1;
					if (isNaN(m)) return null; //failed
				} else {
					for (var l = 0;; ++l) {
						if (l == 12) return null; //failed
						if (len == 3) {
							if (zk.SMON[l].split(/\W+/)[0] == token) {
								m = l;
								break;
							}
						} else {
							if (zk.FMON[l].split(/\W+/)[0] == token) {
								m = l;
								break;
							}
						}
					}
				}
				break;
			case 'd':
				if (nosep) {
					if (len < 2) len = 2;
					if (token.length > len) {
						ts[--i] = token.substring(len);
						token = token.substring(0, len);
					}
				}
				d = parseInt(token, 10);
				if (isNaN(d)) return null; //failed
				break;
			//case 'E': ignored
			}
			j = k - 1;
		}
	}

	var dt = new Date(y, m, d);
	if (strict && (dt.getFullYear() != y
	|| dt.getMonth() != m || dt.getDate() != d))
		return null; //failed
	return dt;
};

/** Generates a formated string for the specified Date object. */
zk.formatDate = function (val, fmt) {
	if (!fmt) fmt = "yyyy/MM/dd";

	var txt = "";
	for (var j = 0; j < fmt.length; ++j) {
		var cc = fmt.charAt(j);
		if (cc == 'y' || cc == 'M' || cc == 'd' || cc == 'E') {
			var len = 1;
			for (var k = j; ++k < fmt.length; ++len)
				if (fmt.charAt(k) != cc)
					break;

			switch (cc) {
			case 'y':
				if (len <= 3) txt += zk.formatFixed(val.getFullYear() % 100, 2);
				else txt += zk.formatFixed(val.getFullYear(), len);
				break;
			case 'M':
				if (len <= 2) txt += zk.formatFixed(val.getMonth()+1, len);
				else if (len == 3) txt += zk.SMON[val.getMonth()];
				else txt += zk.FMON[val.getMonth()];
				break;
			case 'd':
				txt += zk.formatFixed(val.getDate(), len);
				break;
			default://case 'E':
				if (len <= 3) txt += zk.SDOW[val.getDay()];
				else txt += zk.FDOW[val.getDay()];
			}
			j = k - 1;
		} else {
			txt += cc;
		}
	}
	return txt;
};

//-- HTML/XML --//
zk.encodeXML = function (txt, multiline) {
	var out = "";
	if (txt)
		for (var j = 0; j < txt.length; ++j) {
			var cc = txt.charAt(j);
			switch (cc) {
			case '<': out += "&lt;"; break;
			case '>': out += "&gt;"; break;
			case '&': out += "&amp;"; break;
			case '\n':
				if (multiline) {
					out += "<br/>";
					break;
				}
			default:
				out += cc;
			}
		}
	return out
};

/** Returns an integer of the attribute of the specified element. */
zk.getIntAttr = function (el, nm) {
	return parseInt(el.getAttribute(nm) || "0", 10);
};
