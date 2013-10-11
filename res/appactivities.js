var fontSize = 10 / scale;
var lastUid = null;

function renderView(g, uid, view, dx, dy) {
    var x = dx + view.x;
    var y = dy + view.y;
    var visible = ('V' == view.flags0.charAt(0));
    var selected = uid == view.uid;
    if (selected) {
        if (visible) {
            g.fillStyle = 'rgba(255, 0, 0, 0.3)';
            g.strokeStyle = 'rgba(255, 0, 0, 1.0)';
        } else {
            g.fillStyle = 'rgba(255, 0, 255, 0.3)';
            g.strokeStyle = 'rgba(255, 0, 255, 1.0)';
        }
    } else {
        g.fillStyle = 'rgba(0, 255, 0, 0.05)';
        g.strokeStyle = 'rgba(0, 255, 0, 0.4)';
    }
    if (visible || selected) {
        g.fillRect(x, y, view.w, view.h);
        g.strokeRect(x, y, view.w, view.h);
    }
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

function renderViews(uid) {
    var c = document.getElementById('canvas');
    var g = c.getContext('2d');
    g.setTransform(1, 0, 0, 1, 0, 0);
    g.fillStyle = '#000000';
    g.fillRect(0, 0, outW, outH);
    g.scale(scale, scale);
    renderView(g, uid, views, -views.x, -views.y);
}

function showNode(uid) {
    var node = $('#n' + uid);
    while (node != undefined && node.size() != 0) {
        console.log("node=" + node + " size=" + node.size());
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
    var ret = view.uid;
    var cnt = view.children.length;
    for (var i = 0; i < cnt; i++) {
        var child = view.children[i];
        var childRet = findUid(lx, ly, child);
        if (childRet != 0) {
            ret = childRet;
        }
    }
    return ret;
}

function onCanvasClick(canvas, event) {
    var br = canvas.getBoundingClientRect();
    var x = (event.clientX - br.left) / scale;
    var y = (event.clientY - br.top) / scale;
    var uid = findUid(x + views.x, y + views.y, views);
    console.log("Clicked on: " + uid);
    onClick(uid);
    showNode(uid);
}

renderViews(0);
