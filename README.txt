GreasySpoon
Copyright (C) 2008-2011 Karel MITTIG.
----------------------------------------
ICAP server that helps creating value-added services over HTTP. It
can be used to develop generic or specific functions, for example, ad insertion,
virus scanning, content translation, language translation, or content filtering.
GreasySpoon focuses on providing a real-time, easy to use development interface
by handling and hidding most of the HTTP processing mechanism.

=====================================
CREDITS
=====================================
Thanks to Wade, Tamas, Jose, Peter, Pawel, Brad and many other people who helped me to improve this software. 
Special thanks to Samuel for his contribution to the project.

For suggestion, bugfix or code contribution you can contact me at : karel [dot] mittig [at] gmail [dot] com

=====================================
LICENSE
=====================================
This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as
published by the Free Software Foundation, either version 3 of the
License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.


=====================================
GreasySpoon quick installation guide
=====================================
For detailed instructions, see GreasySpoon web site - http://greasyspoon.sourceforge.net

=====================================
1. Prerequisites
=====================================
  - Java JRE 1.6.x
  - 20 Mo free space for default installation
  
=====================================
2. Quick Upgrade for minor releases
=====================================
For minor releases, you can process to a quick update by extracting and overwriting greasyspoon.jar with new version in main directory.
  

=====================================  
3. Installation
=====================================  
- Copy greasyspoon package in desired directory
- untar/unzip greasyspoon package greasyspoon-[version].tar.gz

- edit "greasyspoon" startup script and update JAVA_HOME variable accordingly to server locale
	JAVA_HOME=/usr/lib/jvm/java-6-sun/

- if not already done, set execution rights for greasyspoon script
    chmod u+x greasyspoon

- If you have a proxy to reach internet and For javascripts requiring web access and if GreasySpoon service is installed behind web proxies, configure proxyhost and proxyport parameters in file ./conf/icapserver.conf and uncomment/configure following parameters:
    proxyhost  $proxyip
    proxyport  $proxyport


=====================================
4. Extensions
=====================================  
- By default, only Ecmascript (JavaScript) is natively supported.

- Java support can be enabled either by:
	- using a JDK instead of a JRE to run GreasySpoon
	- or when using a JRE, by installing Java extension available on https://sourceforge.net/projects/greasyspoon/files/extension%20packages%20for%20greasyspoon%20v1.x/


- Ruby extension is also available at https://sourceforge.net/projects/greasyspoon/files/extension%20packages%20for%20greasyspoon%20v1.x/

To add a GreasySpoon extension:
	- download it and copy it into ./pkg directory
	- use './greasyspoon -e list' command to ensure that language extension is reachable and note the PACKAGE ID 
	- as root (!), use './greasyspoon -e install $PACKAGEID' command to install language extension

- To add another language, refer to https://scripting.dev.java.net/

=====================================
5. Execution
=====================================  
- On Linux/unix, run the greasyspoon script: $gs/greasyspoon [start|stop|reload|restart|status]

- On Windows, in GreasySpoon directory:
    - run 'greasyspoon.exe' to launch GreasySpoon control panel
    - or in order to run GreasySpoon manually, use following command in GreasySpoon directory: 'javaw.exe -jar icapserver.jar'

To access to web interface, Open following url in your web browser: http://localhost:8088 
Default login/password are:  admin / admin 

