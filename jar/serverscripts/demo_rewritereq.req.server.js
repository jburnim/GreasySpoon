#rights=ADMIN
//-------------------------------------------------------------------
//This is a GreasySpoon script.
//--------------------------------------------------------------------
//WHAT IT DOES:
// - change user agent
// - remove browser referer 
// - To test with http://www.ericgiguere.com/tools/http-header-viewer.html
//--------------------------------------------------------------------
//==ServerScript==
//@name            RequestSample
//@status off
//@description    Request modification example in Ecmascript.
//@include          http://www\.ericgiguere\.com/.*
//@include          .*
//==/ServerScript==
//
//Available elements provided through ICAP server:
//(note: in ruby, these elements are reachable using static variables (precede names with $) )
//---------------
//requestheader  :  HTTP request header
//httprequest    :  HTTP request body
//user_id        :  user id (login in most cases), or null if no authentication is made by the proxy
//user_name      :  user name (CN  provided through LDAP), or null if no authentication is made by the proxy
//sharedcache    :  hash table(hash<String, Object>) shared between all scripts
//---------------
headerstring = "User-Agent: ";
i = requestheader.indexOf(headerstring) + headerstring .length;
i1 = requestheader.indexOf("\r\n", i);

requestheader = requestheader.substring(0,i) 
         + "GreasySpoon/1.0 (GreasySpoon; U; GreasySpoon 0.5; fr; rv:1.0.0) Gecko/20070914" 
         + requestheader.substring(i1);
headerstring = "Referer";
i = requestheader.indexOf(headerstring);
if (i>0){
  i1 = requestheader.indexOf("\r\n", i) +2;
  requestheader = requestheader.substring(0,i) + requestheader.substring(i1);
}
i = requestheader.indexOf("\r\n\r\n");
requestheader = requestheader.substring(0,i)+ "\r\nX-Custom: "+ sharedcache.get("counter")+requestheader.substring(i);




