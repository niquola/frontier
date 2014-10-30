var l = window.location

var ws = new WebSocket('ws://'+l.host +  l.pathname + '/channel')


var byId = function(id){
  return document.getElementById(id)
}

var out = byId('out')

ws.onmessage = function(e){
  out.innerHTML += e.data
  window.scrollTo(0,document.body.scrollHeight)
}
