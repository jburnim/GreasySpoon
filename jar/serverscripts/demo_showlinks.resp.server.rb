#rights=ADMIN
#--------------------------------------------------------------------
#
#This is a GreasySpoon script.
#
#To install, you need :
#   -jruby
#   -hpricot library
#--------------------------------------------------------------------
#
#WHAT IT DOES:
#
#http://www.google.fr:
#   - show ref links as html tag
#
#--------------------------------------------------------------------
#
#==ServerScript==
#@status off
#@name            Anchors
#@order 0
#@description     Make Anchors Visible using XPath
#@include       .*
#==/ServerScript==
#
require 'rubygems'
require 'hpricot'


#Available elements provided through ICAP server
#puts "---------------"
#puts "HTTP request header: #{$requestheader}"
#puts "HTTP request body: #{$httprequest}"
#puts "HTTP response header: #{$responseheader}"
#puts "HTTP response body: #{$httpresponse}"
#puts "user id (login in most cases): #{$user_id}"
#puts "user name (CN  provided through LDAP): #{$user_name}"
#puts "---------------"

def process(httpresponse)
   document = Hpricot(httpresponse)
    #for each 'a' tag anywhere in the document, with a 'name' attribute...
    document.search('//a[@href]') do |link|
        href = URI(link.attributes['href']) rescue nil
        next unless href && href.host
        link.after '<span style="font-size:8px">[' + href.host + ']</span>'
    end
   
    return "#{document}"
end

$httpresponse = process($httpresponse)


