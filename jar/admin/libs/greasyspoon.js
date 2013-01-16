
//----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
/** intercept <Control><s> key to save file*/
function keyHandler(ev){
    ev = ev || window.event;             // gets the event in ie or ns
    kCode = ev.keyCode || ev.which;   // gets the keycode in ie or ns
    if (ev.ctrlKey && kCode == 19 || ev.ctrlKey && kCode == 83 || ev.ctrlKey && kCode == 115) {    // ctrl+s
        saveFunction() // another function that does something
        return false;  // make it so the browser ignores key combo
    }
    if (ev.ctrlKey &&  kCode == 115) {    // ctrl+s
        return false;  // make it so the browser ignores key combo
    }
    return true;
}
//----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

//----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
/** Save currently edited file using xhr*/
function saveFunction(){ 
        updatecontent();
        jQuery.post("editfile.html"
            , { xhr: "true", operation: "save", content: document.forms['fileform'].content.value , filename: document.forms['fileform'].filename.value}
            ,function(data){
                document.getElementById("commentcontent").innerHTML = data.commentcontent;
                document.getElementById("errorcontent").innerHTML = data.errorcontent;
                animateComments();
            }
            , "json"
        );
}
//----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

var editarea_fullscreen = false;
var resize_backup =  "";
function toggleFrames(){
    if (editarea_fullscreen){//restore
        window.parent.document.getElementById('mainframe').setAttribute('cols', '170,*');
        window.parent.document.getElementById('topframe').setAttribute('rows', '120,*');
        document.body.height = '90%';
        //document.getElementById("properties").setAttribute('margin-top', '40px');       
        //document.getElementById("properties").style.display='visible';
        window.frames['frame_coloredtextarea'].document.getElementById("resize_area").innerHTML = resize_backup;
    } else {//size to full screen
       resize_backup =  window.frames['frame_coloredtextarea'].document.getElementById("resize_area").innerHTML;
       window.parent.document.getElementById('mainframe').setAttribute('cols', '0,*');
       window.parent.document.getElementById('topframe').setAttribute('rows', '0,*');
       document.body.style.height='100%';
       document.body.style.margin='0 0 0 0';
       document.getElementById('frame_coloredtextarea').style['min-height']= document.body.clientHeight+"px";
       window.frames['frame_coloredtextarea'].document.getElementById("resize_area").style.visibility='visible';
    }
    editarea_fullscreen = !editarea_fullscreen;
}

//----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
/** callback function to bring a hidden box back */
    function animateCallback(){
        var errorcontent = document.getElementById("errorcontent").innerHTML;
        if (errorcontent == "") {
            setTimeout(function(){
                jQuery("#effect:visible").removeAttr('style').hide().fadeOut();
                if (editarea_fullscreen) {
                    window.frames['frame_coloredtextarea'].document.getElementById("statuscomment").innerHTML = "";
                    window.frames['frame_coloredtextarea'].document.getElementById("statuserror").innerHTML = "";
                }
            }, 1000);
        } else {
            document.getElementById("commentcontent").innerHTML = "";
            if (editarea_fullscreen) window.frames['frame_coloredtextarea'].document.getElementById("statuscomment").innerHTML = "";
        }
        
    }
//----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

//----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------    
/** callback function to bring a hidden box back */
    //run the currently selected effect
    function animateComments(){
        //get effect type from 
        var selectedEffect = "pulsate";
        var errorcontent = document.getElementById("errorcontent").innerHTML;
        var commentcontent = document.getElementById("commentcontent").innerHTML;
        var options = {times:2};
        if (errorcontent == "" && commentcontent=="") { return;}
        if (!editarea_fullscreen){
            //run the effect
            jQuery("#effect").show(selectedEffect,options,600,animateCallback);
        } else {
            //use status bar of editarea to display message
            window.frames['frame_coloredtextarea'].document.getElementById("statuscomment").innerHTML = commentcontent;
            window.frames['frame_coloredtextarea'].document.getElementById("statuscomment").style['color'] = "#3A4392";
            window.frames['frame_coloredtextarea'].document.getElementById("statuserror").innerHTML = errorcontent;
            window.frames['frame_coloredtextarea'].document.getElementById("statuserror").style['color'] = "#E60003";
            jQuery(window.frames['frame_coloredtextarea'].document.getElementById("statuseffect")).show(selectedEffect,options,600,animateCallback);
        }
    }
//----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------    

 function showHelp(){
    jQuery("#showhelp").dialog({ resizable: false });
    jQuery("#showhelp").dialog('open');
 }
 
//----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
/** Initialisation*/
jQuery(function() {
    jQuery("#effect").hide();
    if (navigator.appName == "Microsoft Internet Explorer")
        document.body.onload = animateComments;
    else
        window.onload = animateComments();

});
//----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------