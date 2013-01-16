#rights=ADMIN
//------------------------------------------------------------------- 
// This is a GreasySpoon script.
// --------------------------------------------------------------------
// WHAT IT DOES:
// --------------------------------------------------------------------
// ==ServerScript==
// @name            JS Hello world
// @status off
// @description     Modification script sample in Javascript
// @include    .*     
// @exclude        
// @responsecode    200
// ==/ServerScript==
// --------------------------------------------------------------------
// Available elements provided through ICAP server:
// ---------------
// requestedurl  :  (String) Requested URL
// requestheader  :  (String)HTTP request header
// responseheader :  (String)HTTP response header
// httpresponse   :  (String)HTTP response body
// user_id        :  (String)user id (login or user ip address)
// user_group     :  (String)user group or user fqdn
// sharedcache    :  (hashtable<String, Object>) shared table between all scripts
// trace           :  (String) variable for debug output - requires to set log level to FINE
// ---------------
//trace = responseheader;

//Find html body
a1 = httpresponse.indexOf("<body");
a2 = httpresponse.indexOf(">",a1)+1;

// create / retrieve a transient variable called counter and increased it
i = sharedcache.get("counter");
i++;

//update response
httpresponse = httpresponse.substring(0,a2)
    +"<p style='position:relative; border:1px solid #999999;background:url(\"http://greasyspoon.sourceforge.net/img/logo-copyrighted.gif\")  no-repeat scroll right center #FFFFFF;font-size:12px;font-weight:normal;color: #999999;z-index:100000;margin:0 10 5 10;padding-left: 10px;'>Hello Bonjour hallo päivää salâm ohayô gozaimasu<br />"
    + "Your user ID: "+ user_id  + "<br />"
    +" Your user group: "+user_group+" <br />"
    +"That's your "+i+" request<br />"
    +"Requested URL:"+requestedurl  +"<br /></p>" 
    +httpresponse.substring(a2);

//store updated counter value
sharedcache.put("counter", i);

//insert a custom header
a1 = responseheader.indexOf("\r\n\r\n");
responseheader = responseheader.substring(0,a1) + "\r\nX-Powered-By: Greasyspoon" + responseheader.substring(a1);

//Finished





















