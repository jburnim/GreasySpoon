#rights=ADMIN
//------------------------------------------------------------------- 
// Based on Ewan Sadie script, 2010
// --------------------------------------------------------------------
// WHAT IT DOES: add a link to Keepvid to download YouTube video
// --------------------------------------------------------------------
// ==ServerScript==
// @name            Youtube download
// @status off
// @description Download YouTube via Keepvid
// @include http://.*youtube\.com/watch.*
// ==/ServerScript==

var a1 = httpresponse.indexOf("<span id=\"eow-title\"");

var a2 = httpresponse.indexOf("</span>",a1)+"</span>".length;

//var webgrabber = new Array("Keepvid","http://keepvid.com/?url=http%3A%2F%2Fwww.youtube.com%2Fwatch%3Fv%3D");
var webgrabber = new Array("Force-download","http://www.force-download.net/?lang=fr&video_url=http%3A%2F%2Fwww.youtube.com%2Fwatch%3Fv%3D");


function extract(param) {
    var params= requestedurl.substring(requestedurl.indexOf("?") + 1, requestedurl.length);
    var p1 = params.indexOf(param+"=")+param.length + 1;
    var p2 = params.indexOf("&",p1);
    if (p2==-1) p2 = params.length;
    return params.substring(p1, p2);
}
trace = a1 + " : "+ a2;


httpresponse = httpresponse.substring(0,a2)
       +"<button type='button' onclick=\"window.open('"+webgrabber[1] 
       + extract("v") 
       +"','_blank')\" title='Download via "+webgrabber[0]
       +"' class='yt-uix-button yt-uix-tooltip' id='subscribeDiv'><span class='yt-uix-button-content'>Download</span></button>" 
       +httpresponse.substring(a2);


trace = httpresponse ;

















