var l = window.location

var ws = new WebSocket('ws://'+l.host +  '/builds/channel')


var byId = function(id){
  return document.getElementById(id)
}

var byClass = function(cls){
  return document.getElementsByClassName(cls);
}

var out = byId('builds')


var forEach = Array.prototype.forEach;

var nodes = byClass("status")

ws.onmessage = function(e){
  var ps = JSON.parse(e.data)
  var actives = ps.map(function(x){
    return x.name
  })

  forEach.call(nodes, function(el){
    console.log(el.id, el)
    if(actives.indexOf(el.id) > -1){
      el.classList.add('building')
    }else{
      el.classList.remove('building')
    }
  })
}
