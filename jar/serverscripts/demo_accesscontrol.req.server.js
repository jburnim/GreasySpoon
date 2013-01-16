#rights=USER
//-------------------------------------------------------------------
//This is a GreasySpoon script.
//--------------------------------------------------------------------
//WHAT IT DOES:
//  - Request filter that sends back a direct response for given domain
//--------------------------------------------------------------------
//==ServerScript==
//@name            FilterRequests
//@status off
//@order           1
//@timeout        100
//@description    Intercept user request and return directly a response when matching regex in include tag
//@include         http://.*game.*
//@exclude        
//==/ServerScript==
//
//Available elements provided through ICAP server:
//(note: in ruby, these elements are reacheable using static variables (preceed names with $) )
//---------------
//requestheader  :  (String)HTTP request header
//httprequest    :  (String)HTTP request body
//user_id        :  (String)user id (login or user ip address)
//user_group     :  (String)user group or user fqdn
//sharedcache    :  (hashtable<String, Object>) shared table between all scripts
//---------------
requestheader="HTTP/1.1 200 OK\r\nContent-Type: text/html;charset=ISO-8859-1\r\n\r\n";
httprequest   = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\"><html><head><title>direct response example</title></head>"
         + "<body><h1>You should go back to work !!</h1>"
         + "</body>"
         + "</html>";



