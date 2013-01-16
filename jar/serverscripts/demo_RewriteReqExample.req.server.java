#rights=ADMIN
//------------------------------------------------------------------- 
// ==ServerScript==
// @name            rewriteHeaders
// @status off
// @description     request modification example in java.
// @include         http://www\.ericgiguere\.com/tools/http-header-viewer\.html.*
// @include         .*
// ==/ServerScript==
// --------------------------------------------------------------------
// Note: use httpMessage object methods to manipulate HTTP Message
// use debug(String s) method to trace items in service log (with log level >=FINE)
// ---------------
 
// ---------------
 public void main(HttpMessage httpMessage){
     //start your code from here
     //System.err.println(httpMessage.getUsername());
     //System.err.println(httpMessage.getUsergroup());
     //System.err.println(httpMessage.getRequestHeaders());
     //System.err.println(httpMessage.getBody());  
   if (httpMessage.getUrl().startsWith("http://www.ericgiguere.com/tools/http-header-viewer.html")){
        debug(httpMessage.getRequestHeaders());
        httpMessage.rewriteHeader("User-Agent","GreasySpoon Power !!");
        httpMessage.deleteHeader("pragma");
        httpMessage.deleteHeader("accept-language");
        httpMessage.addHeader("X-test1", "test01");
        httpMessage.addHeader("X-test2", "test02");
        httpMessage.rewriteHeader("X-test1", "test001");
        httpMessage.rewriteHeader("X-test1", "test0-01");
        httpMessage.rewriteHeader("X-test1", "test00-1");
        httpMessage.rewriteHeader("Referrer", "http://outerspace.galaxy/");
        httpMessage.setUrl("http://www.ericgiguere.com/tools/http-header-viewer.html?testurl=tata");
        debug("+++++++++++++\r\n"+httpMessage.getRequestHeaders());
    } else {
        httpMessage.addHeader("X-Powered-By", "GreasySpoon");
    }
 }
















