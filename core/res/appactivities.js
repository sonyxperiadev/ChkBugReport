var fontSize = 10 / scale;
var lastUid = null;

function renderView(g, uid, view, dx, dy) {
    var x = dx + view.x;
    var y = dy + view.y;
    var visible = ('V' == view.flags0.charAt(0) || view == views);
    var selected = uid == view.uid;
    if (view.selected) {
        if (view.selected == 2) {
            g.fillStyle = 'rgba(255, 0, 0, 0.3)';
            g.strokeStyle = 'rgba(255, 0, 0, 0.7)';
        } else {
            g.fillStyle = 'rgba(255, 0, 255, 0.05)';
            g.strokeStyle = 'rgba(255, 0, 255, 0.4)';
        }
    } else {
        if (visible) {
            g.fillStyle = 'rgba(0, 255, 0, 0.05)';
            g.strokeStyle = 'rgba(0, 255, 0, 0.4)';
        } else {
            return;
        }
    }
    g.fillRect(x, y, view.w, view.h);
    g.strokeRect(x, y, view.w, view.h);
    if (uid == view.uid) {
        g.fillStyle = 'rgba(255, 255, 255, 0.7)';
        g.font = '' + fontSize + 'px Arial';
        g.fillText(view.name, x, y + fontSize);
        if (view.id != null) {
            g.fillText(view.id, x, y + 2*fontSize);
        }
    }
    var cnt = view.children.length;
    for (var i = 0; i < cnt; i++) {
        var child = view.children[i];
        renderView(g, uid, child, x, y);
    }
}

function markSelected(uid, view) {
    view.selected = (uid == view.uid ? 2 : 0);
    var cnt = view.children.length;
    for (var i = 0; i < cnt; i++) {
        var child = view.children[i];
        if (markSelected(uid, child)) {
            view.selected = 1;
        }
    }
    return view.selected;
}

function renderViews(uid) {
    var c = document.getElementById('canvas');
    var g = c.getContext('2d');
    g.setTransform(1, 0, 0, 1, 0, 0);
    g.fillStyle = '#000000';
    g.fillRect(0, 0, outW, outH);
    g.scale(scale, scale);
    markSelected(uid, views);
    renderView(g, uid, views, -views.x, -views.y);
}

function showNode(uid) {
    var node = $('#n' + uid);
    while (node != undefined && node.size() != 0) {
        if (node.hasClass("tree")) break;
        if (node.hasClass("jstree-closed")) {
            $(".tree").jstree("open_node", node);
        }
        node = node.parent();
    }
}

function onClick(uid) {
    $('#n' + lastUid).css({ background: 'inherit', color: 'inherit' });
    $('#n' + uid).css({ background: '#4060ff', color: '#ffffff' });
    renderViews(uid);
    lastUid = uid;
}

function findUid(x, y, view) {
    if ('V' != view.flags0.charAt(0) && view != views) return 0; // Not visible
    var lx = x - view.x;
    var ly = y - view.y;
    if (lx < 0 || ly < 0 || lx >= view.w || ly >= view.h) return 0;
    var cnt = view.children.length;
    var ret = [];
    for (var i = 0; i < cnt; i++) {
        var child = view.children[i];
        var childRet = findUid(lx, ly, child);
        if (childRet != 0) {
            ret = ret.concat(childRet);
        }
    }
    if (ret.length == 0) {
        return [ view.uid ];
    } else {
        return ret;
    }
}

function onCanvasClick(canvas, event) {
    var br = canvas.getBoundingClientRect();
    var x = (event.clientX - br.left) / scale;
    var y = (event.clientY - br.top) / scale;
    var uid = findUid(x + views.x, y + views.y, views);
    if (uid.length == 0) {
        return; // Nothing clicked on
    }
    if (uid.length > 1) {
        // Several items, pick the next one
        var oldIdx = uid.indexOf(lastUid);
        if (oldIdx < 0 || oldIdx == uid.length - 1) {
            uid = uid[0];
        } else {
            uid = uid[oldIdx + 1];
        }
    }
    onClick(uid);
    showNode(uid);
}

renderViews(0);
