var fontSize = 10 / scale;
var lastUid = null;

function renderView(g, uid, view, dx, dy) {
    var x = dx + view.x;
    var y = dy + view.y;
    if (uid == view.uid) {
        g.fillStyle = 'rgba(255, 0, 0, 0.3)';
        g.strokeStyle = 'rgba(255, 0, 0, 1.0)';
    } else {
        g.fillStyle = 'rgba(0, 255, 0, 0.05)';
        g.strokeStyle = 'rgba(0, 255, 0, 0.4)';
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

function renderViews(uid) {
    var c = document.getElementById('canvas');
    var g = c.getContext('2d');
    g.setTransform(1, 0, 0, 1, 0, 0);
    g.fillStyle = '#000000';
    g.fillRect(0, 0, outW, outH);
    g.scale(scale, scale);
    renderView(g, uid, views, -views.x, -views.y);
}

function onClick(uid) {
    $('#n' + lastUid).css({ background: 'inherit', color: 'inherit' });
    var node = $('#n' + uid);
    node.css({ background: '#4060ff', color: '#ffffff' });
    while (true) {
        if (node.hasClass("tree")) break;
        if (node.hasClass("jstree-closed")) {
            $(".tree").jstree("open_node", node);
        }
        node = node.parent();
    }
    renderViews(uid);
    lastUid = uid;
}

function findUid(x, y, view) {
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
    onClick(findUid(x + views.x, y + views.y, views));
}

renderViews(0);
