/**----------------------------------------------------------------------------
 * GreasySpoon
 * Copyright (C) 20010 Karel Mittig
 *-----------------------------------------------------------------------------
 */

var url;
var startname;
var tailing = false;


function switchTail(urltofetch) {
	if (tailing){
		stopTail();
		return;
	}
	url = urltofetch;
	getLog();
	startname = $('#tailbutton').html();
	$('#tailbutton').html("...tailing...");
	tailing = true;
}


function getLog() {
	$.get(url, function(data){
		if (data !== "") $('#logdiv').prepend(data+"\n");
	 });
	refresh();
}

function refresh() {
	timer = setTimeout("getLog()", 1000);
}

function stopTail() {
	tailing = false;
	clearTimeout(timer);
	$('#tailbutton').html(startname);
}

var timer;
$(function() {
	if (top.location==document.location){
		$('#properties').css("margin", "0 5px 0 0");
		$('#properties').css("width", "100%");
		$('#properties').css("heigth", "100%");
		$('body').css("margin", "5px");
		$('.listing').css("height", "100%");
		$('.listing').css("margin-bottom", "10px");
		$('.listing').css("padding-bottom", "10px");
		$('#expandbutton').css("visibility","hidden");      
	} else {
	}
	
	
	 //all hover and click logic for buttons
	 $(".fg-button:not(.ui-state-disabled)").hover(
	 	function(){
	 		$(this).addClass("ui-state-hover");
	 	},
	 	function(){
	 		$(this).removeClass("ui-state-hover");
	 	}
	 	)
	 	.mousedown(function(){
	 		$(this).parents('.fg-buttonset-single:first').find(".fg-button.ui-state-active").removeClass("ui-state-active");
	 		if( $(this).is('.ui-state-active.fg-button-toggleable, .fg-buttonset-multi .ui-state-active') ){
	 			 $(this).removeClass("ui-state-active"); 
	 		}  else { 
	 			$(this).addClass("ui-state-active"); 
	 		}
	 	})
	 	.mouseup(function(){
	 		if(! $(this).is('.fg-button-toggleable, .fg-buttonset-single .fg-button, .fg-buttonset-multi .fg-button') ){
	 		$(this).removeClass("ui-state-active");
	 	}
	 });


});




