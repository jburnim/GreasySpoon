function TreeMenu_select(b,og) { 
 var i,s,c,k,j,tN,hh;if(document.getElementById){
 if(b.parentNode && b.parentNode.childNodes){tN=b.parentNode.childNodes;}else{return;}
 for(i=0;i<tN.length;i++){if(tN[i].tagName=="DIV"){s=tN[i].style.display;
 hh=(s=="block")?"none":"block";if(og==1){hh="block";}tN[i].style.display=hh;}}
 c=b.firstChild;if(c.data){k=c.data;j=k.charAt(0);if(j=='+'){k='-'+k.substring(1,k.length);
 }else if(j=='-'){k='+'+k.substring(1,k.length);}c.data=k;}if(b.className=='treeplusmark'){
 b.className='treeminusmark';}else if(b.className=='treeminusmark'){b.className='treeplusmark';}}
}

function TreeMenu_setMenu(){ 
 var i,d='',h='<style type=\"text/css\">';
 if(document.getElementById){
 	var tA=navigator.userAgent.toLowerCase();
 	try{
	 	if(window && typeof window["opera"] != "undefined" ){
			if(tA.indexOf("opera 5")>-1 || tA.indexOf("opera 6")>-1){return;}
		}
	}catch (e){}
 	for(i=1;i<20;i++){d+='div ';h+="\n#treeMenuItem div "+d+"{display:none;}";}
 	document.write(h+"\n</style>");
 }
}
TreeMenu_setMenu();

function TreeMenu_init(){ 
 var i,x,d,hr,ha,ef,a,ag;if(document.getElementById){d=document.getElementById('treeMenuItem');
 if(d){hr=window.location.href;ha=d.getElementsByTagName("A");if(ha&&ha.length){
 for(i=0;i<ha.length;i++){if(ha[i].href){if(hr.indexOf(ha[i].href)>-1){
 ha[i].className="treecurrentmark";a=ha[i].parentNode.parentNode;while(a){
 if(a.firstChild && a.firstChild.tagName=="A"){if(a.firstChild.onclick){
 ag=a.firstChild.onclick.toString();if(ag&&ag.indexOf("TreeMenu_select")>-1){
 TreeMenu_select(a.firstChild,1);}}}a=a.parentNode;}}}}}}}
}

function TreeMenu_collapse(a){ 
 var i,x,ha,s,tN;if(document.getElementById){ha=document.getElementsByTagName("A");
 for(i=0;i<ha.length;i++){if(ha[i].onclick){ag=ha[i].onclick.toString();
 if(ag&&ag.indexOf("TreeMenu_select")>-1){if(ha[i].parentNode && ha[i].parentNode.childNodes){
 tN=ha[i].parentNode.childNodes;}else{break;}for(x=0;x<tN.length;x++){
 if(tN[x].tagName=="DIV"){s=tN[x].style.display;if(a==0&&s!='block'){TreeMenu_select(ha[i]);
 }else if(a==1&&s=='block'){TreeMenu_select(ha[i]);}break;}}}}}}
}

var currentmark = null;
function TreeMenu_create(){ 
	var i,x,d,tN,ag;
	if(document.getElementById){
		d=document.getElementById('treeMenuItem');
		if(d){
			tN=d.getElementsByTagName("A");
			if(tN&&tN.length){
				for(i=0;i<tN.length;i++){
					ag=(tN[i].onclick)?tN[i].onclick.toString():false;
					if(ag&&ag.indexOf("TreeMenu_select")>-1){
						tN[i].className='treeplusmark';
					} else {
						if (tN[i].className!="treecurrentmark") {
							tN[i].className='treedefmark';
						} else {
							currentmark = tN[i];
						}
						tN[i].onclick = TreeMenu_setCurrent;
					}
				}
			}
		}
	}
}

function TreeMenu_setCurrent(e,x) {
	if (currentmark!=null) currentmark.className='treedefmark';
	e = e || window.event;
	x = x || this;
	x.className='treecurrentmark';
	currentmark = x;
}

function TreeMenu_findObj(n, d) { 
 var p,i,x;  if(!d) d=document; if((p=n.indexOf("?"))>0&&parent.frames.length) {
 d=parent.frames[n.substring(p+1)].document; n=n.substring(0,p);}
 if(!(x=d[n])&&d.all) x=d.all[n]; for (i=0;!x&&i<d.forms.length;i++) x=d.forms[i][n];
 for(i=0;!x&&d.layers&&i<d.layers.length;i++) x=TreeMenu_findObj(n,d.layers[i].document);
 if(!x && d.getElementById) x=d.getElementById(n); return x;
}

function TreeMenu_setNV(){
 //set the image over and down name convention
 document.treeNavOver="_over";
 document.treeNavDown="_down";
 var i,k=-1,g,x,gg,tl,ti,tm,tt,tu,args=TreeMenu_setNV.arguments,dt=false;
 if(document.getElementsByTagName){dt=true;}if(document.TreeMenuNavBar){return;}
 TreeMenuNavProp=new Array();for(i=0;i<args.length;i++){TreeMenuNavProp[i]=args[i];}
 TreeMenuNavIM=new Array();if(dt){tm=document.getElementsByTagName("IMG");
 }else{tm=document.images;}tm=document.images;tt=new Array();tt=tt.concat(tm);
 if(document.layers){for(i=0;i<document.layers.length;i++){
 ti=document.layers[i].document.images;if(ti){tt=tt.concat(ti);}
 for(x=0;x<document.layers[i].document.layers.length;x++){
 ti=document.layers[i].document.layers[x].document.images;if(ti){tt=tt.concat(ti);
 }}}tm=tt;}for(i=0;i<tm.length;i++){tl=tm[i].name;if(dt&&!tl){tl=tm[i].id;}
 if(tl&&tl.indexOf("treeNVim")==0){k++;TreeMenuNavIM[k]=tl;}}document.TreeMenu_NVswapd=new Array();
 document.TreeMenu_NVswapo=new Array();for(i=0;i<TreeMenuNavIM.length;i++){g=TreeMenu_findObj(TreeMenuNavIM[i]);
 gg=g.src;g.treeNVim=g.src;tu=gg.lastIndexOf(".");
 g.treeNVimo=gg.substring(0,tu)+document.treeNavOver+gg.substring(tu,gg.length);
 g.treeNVimd=gg.substring(0,tu)+document.treeNavDown+gg.substring(tu,gg.length);
 if(TreeMenuNavProp[1]>1){document.TreeMenu_NVswapo[i]=new Image();document.TreeMenu_NVswapo[i].src=g.treeNVimo;}
 if(TreeMenuNavProp[1]>0){if(TreeMenuNavProp[1]==3){g.treeNVimd=g.treeNVimo;}
 document.TreeMenu_NVswapd[i]=new Image();document.TreeMenu_NVswapd[i].src=g.treeTBimd;}}
 document.TreeMenuNavBar=true;
}
function TreeMenu_trigNV(bu){ 
 if(!document.TreeMenuNavBar){return;}var i,d,dB=-1,tF=false,sF=false;
 for(i=0;i<TreeMenuNavIM.length;i++){d=TreeMenu_findObj(TreeMenuNavIM[i]);
 if(TreeMenuNavIM[i]==TreeMenuNavProp[0]){dB=i;}if(TreeMenuNavIM[i]==bu){tF=true;
 if(TreeMenuNavProp[1]>0){if(i==dB){d.src=d.treeNVimd;}else if (TreeMenuNavProp[1]>1){
 d.src=d.treeNVimo;}}}else{if(TreeMenuNavProp[1]>0){d.src=d.treeNVim;}}}
 if(!tF){if(dB>-1){d=TreeMenu_findObj(TreeMenuNavIM[dB]);if(TreeMenuNavProp[1]>0){d.src=d.treeNVimd;}}}
}
