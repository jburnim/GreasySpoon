#rights=ADMIN
//------------------------------------------------------------------- 
// Created by Ewan Sadie, 2010
// --------------------------------------------------------------------
// ==ServerScript==
// @name            safe search
// @status off
// @description Force "Safe Search" on search engines and YouTube
// @include .*\.google\..*
// @include .*\.youtube\..*
// @include .*www\.altavista\.com.*
// @include .*www\.ask\.co.*
// @include .*bing\.com.*
// @exclude .*wzus\.ask\.com.*
// @exclude .*mail\.google\.com.*
// @exclude .*mystuff\.ask\.com.*
//==/ServerScript==
//
var URIstring = requestedurl;
headerstring = "GET ";

if (requestheader.indexOf(headerstring)!=-1) {
    i2 = requestheader.indexOf(headerstring) + headerstring .length;
    i3 = requestheader.indexOf("HTTP/1", i2)-1;
    
    headerstring = "Cookie: ";
    c = requestheader.indexOf(headerstring) + headerstring .length;
    c1 = requestheader.indexOf("\r\n", c);
    var Cookiestring = requestheader.substring(c,c1);
    
    SafeSearch = "";
    if (URIstring.indexOf("?")>0){
        //Google
        if (URIstring.indexOf(".google.")>0){
            if (URIstring.indexOf("safe=off")>0){
                URIstring = URIstring.replace("safe=off","safe=on");
            } else {
                SafeSearch = "&safe_search=on&safe=on";
            }
        }
        
        //YouTube
        if (URIstring.indexOf(".youtube.com")>0){
            if (URIstring.indexOf("safe_search=on")!=-1){
                SafeSearch = "&safe_search=on&safety_mode=true&persist_safety_mode=1";
            }
            if (URIstring.indexOf("safety_mode=false")>0){
                URIstring = URIstring.replace("safety_mode=false","safety_mode=true");
            }
        }
        
        //Altavista
        if (URIstring.indexOf("altavista.com")>0){
            if (Cookiestring.indexOf("AV_ALL=1")>0){
                Cookiestring = Cookiestring.replace("AV_ALL=1","AV_PG=1");
                requestheader = requestheader.substring(0,c) + Cookiestring + requestheader.substring(c1);
            } else {
                requestheader = requestheader.substring(0,c) + Cookiestring + "; AV_PG=1" + requestheader.substring(c1);
            }
        }
        
        //Ask
        if (URIstring.indexOf("ask.co")>0){
            if (Cookiestring.indexOf("adlt=")>0 || Cookiestring.indexOf("adt=")>0){
                Cookiestring = Cookiestring.replace("adlt=1","adlt=0");
                Cookiestring = Cookiestring.replace("adt=1","adt=0");
                requestheader = requestheader.substring(0,c) + Cookiestring + requestheader.substring(c1);
            } else {
                requestheader = requestheader.substring(0,c) + Cookiestring + ";adlt=0;adt=0" + requestheader.substring(c1);
            }
        }
        
        //Bing
        if (URIstring.indexOf("bing.com")>0){
            if (URIstring.indexOf("adlt=")>0){
                URIstring = URIstring.replace("adlt=off","adlt=strict");
            } else {
                SafeSearch = "&adlt=strict";
            }
            if (Cookiestring.indexOf("adlt=")>0 || Cookiestring.indexOf("adt=")>0){
                Cookiestring = Cookiestring.replace("ADLT=OFF","ADLT=STRICT");
                Cookiestring = Cookiestring.replace("ADLT=DEMOTE","ADLT=STRICT");
                requestheader = requestheader.substring(0,c) + Cookiestring + requestheader.substring(c1);
            } else {
                requestheader = requestheader.substring(0,c) + Cookiestring + ";adlt=strict" + requestheader.substring(c1);
            }
        }
    }
    
    requestheader = requestheader.substring(0,i2) + URIstring + SafeSearch + requestheader.substring(i3);
    }
