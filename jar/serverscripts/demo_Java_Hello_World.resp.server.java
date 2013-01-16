#rights=ADMIN
//------------------------------------------------------------------- 
// ==ServerScript==
// @name            hello
// @status off
// @description     Simple Hello World example in java
// @include         .*
// @responsecode    200
// ==/ServerScript==
// --------------------------------------------------------------------
// Note: use httpMessage object methods to manipulate HTTP Message
// use debug(String s) method to trace items in service log (with log level >=FINE)
// ---------------



public void main(HttpMessage httpMessage){
    try{
       //System.err.println(httpMessage.getRequestHeaders());
       //System.err.println(httpMessage.getResponseHeaders());
       //System.err.println(httpMessage.getBody());
       if (httpMessage.getResponseHeader("Content-Type").contains("text/html")) {   
           String body = httpMessage.getBody();
           if ( body.indexOf("<body") <=0) return;
           int pos1 = body.indexOf(">", body.indexOf("<body"))+1;
           body = body.substring(0,pos1) + "<h1>hello Java world €ëùçàöâê </h1>" + body.substring(pos1);
           
           //reconfigure output encoding
           pos1 = body.indexOf("<meta http-equiv=\"Content-Type\"");
           if (pos1==-1) pos1 = body.indexOf("<meta http-equiv=\"content-type\"");
           if (pos1>0){
              int pos2 = body.indexOf("/>", pos1)+2;
              body = body.substring(0,pos1) + body.substring(pos2);
           }

           pos1 = body.indexOf("<?xml version=\"1.0\" encoding=\"");
           if (pos1!=-1){
              pos1 = pos1+"<?xml version=\"1.0\" encoding=\"".length();
              int pos2 = body.indexOf("\"?>", pos1);
              body = body.substring(0,pos1)+"UTF-8" + body.substring(pos2);
           }
           httpMessage.rewriteHeader("Content-Type","text/html; charset=UTF-8");
           
           httpMessage.setBody(body);
      }
      // httpMessage.toJson();
      //httpMessage.minify();
//      httpMessage.addHeader("X-UA-Compatible", "IE=IE7");
      httpMessage.addHeader("X-Powered-by", "GreasySpoon");
    }catch (Exception e){
       debug(e.getMessage());
    }
}
















