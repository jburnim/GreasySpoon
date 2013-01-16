#rights=USER
#-------------------------------------------------------------------
#
#This is a GreasySpoon script.
#
#To install, you need 
#   - jruby
#   - hpricot ruby library
#--------------------------------------------------------------------
#
#WHAT IT DOES:
#
#http://www.google.fr:
#- Replace Google logo by Orange logo 
#- Rename logo id to avoid page scripts to restore it
#- load adverts and css from orange.fr homepage and insert them into page
#--------------------------------------------------------------------
#
#==ServerScript==
#@name            CustomizeGoogle
#@description     Customize Google page by replacing ads/changing logo
#@status off
#@include       http://www\.google\..{2,3}/.*
#==/ServerScript==
#
#Available elements provided through ICAP server
#puts "---------------"
#puts "HTTP request header: #{$requestheader}"
#puts "HTTP request body: #{$httprequest}"
#puts "HTTP response header: #{$responseheader}"
#puts "HTTP response body: #{$httpresponse}"
#puts "user id (login in most cases): #{$user_id}"
#puts "user name (CN  provided through LDAP): #{$user_name}"
#puts "---------------"

require 'rubygems'
require 'hpricot'
require 'open-uri'

def process(httpresponse)
   doc = Hpricot(httpresponse)
   
   #change background color
   (doc/"body").set("bgcolor", "#000000")
   
   # Load banner from another web server page and insert it into page
   doc1 = Hpricot(open('http://greasyspoon.sourceforge.net/download.html'))
   addon = (doc1/"#droite")
   (addon/"#droite").prepend("<img border=\"0\" src=\"http://greasyspoon.sourceforge.net/img/logo-copyrighted.gif\"/>")
   addon = addon.to_s().gsub("src=\"img", "src=\"http://greasyspoon\.sourceforge\.net/img")
   (doc/"#res").prepend("#{addon}")

   # Load css associated to banner from another web server page and insert it into page 
   #doc2 = Hpricot(URI.parse('http://i5.woopic.com/Css/hp.css?149').read)
   doc2 =  Hpricot(open('http://greasyspoon.sourceforge.net/css/gs.css'))
   addon = doc2.to_s()
   addon = "<style>"+addon +"</style>"  
   (doc/"head").append("#{addon}") rescue nil

   #change Google logo by another
   (doc/"#logo").set("id", "loga")
   (doc/"#loga").inner_html = "<img border=\"0\" src=\"http://greasyspoon.sourceforge.net/img/gs.png\"/>"
   #return modified content
   return "#{doc}"
end

$httpresponse = process($httpresponse)

