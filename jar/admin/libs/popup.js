/**
 * jquery flower bubble
 *
 * blocks and tints the screen, draws a bubble and a flower inside of it
 *
 * @author Oktay Acikalin ( ok@ryotic.de )
 * @version 0.4
 * @license MIT / GPL v2
 * @copyright Oktay Acikalin, 7 October, 2008
 * @package jquery_popup
 * 
 * example call:
 * 
 * var flobu ;
 * 
 * $(function(){
 * 	flobu = new popup ({
 * 		base_obj: $( 'div#some_obj' ),
 * 		base_dir: _appldir + '/images',
 * 		background: { css: 'white', opacity: 0.78 },
 * 		bubble: { image: 'bubble.png', width: 130, height: 98 },
 * 		flower: { image: 'flower.gif', width: 32, height: 32 }
 * 	}) ;
 * }) ;
 * 
 * flobu . enable () ;
 * flobu . disable () ;
 * 
 **/
var popup = function ( options ) {
	
	// begin: define defaults
	this . defaults = {
		base_obj: null, // which (jquery-)object to block
		
		container_id: 'popup_container',
    
		fade_speed: 'normal',
		zindex: 1000,
		
		// full = start at base_obj.top and cover whole screen
		// base_obj = only block this specific object
		block_mode: 'full',
		// block_mode: 'base_obj',
    
		base_dir: 'images',
		background: { css: 'white', opacity: 0.5 },
		bubble: { image: 'bubble.png', width: 'auto', height: 'auto' },
		flower: { image: 'flower.gif', width: 'auto', height: 'auto' }
	} ;
	// end: define defaults
	
	// begin: add some timestamp to the id to make it quite unique
	var t = new Date () ;
	this . defaults . container_id += '_' + t . getTime () ;
	// end: add some timestamp to the id to make it quite unique
	
	// begin: define container vars
	this . base_obj ;
	this . container_id ;
	this . fade_speed ;
	this . zindex ;
	this . block_mode ;
	this . base_dir ;
	this . background ;
	this . bubble ;
	this . flower ;
	// end: define container vars
	
	// begin: setup container vars
	jQuery . extend ( this . defaults, options ) ;
	jQuery . extend ( this, this . defaults ) ;
	// end: setup container vars
	
	// begin: preload images
	var bubble_img = $( '<img src="' + this . base_dir + '/' + this . bubble . image + '">' ) ;
	var flower_img = $( '<img src="' + this . base_dir + '/' + this . flower . image + '">' ) ;
	if ( this . bubble . width == 'auto' || this . bubble . width == undefined )
		this . bubble . width = bubble_img . get ( 0 ) . width ;
	if ( this . bubble . height == 'auto' || this . bubble . height == undefined )
		this . bubble . height = bubble_img . get ( 0 ) . height ;
	if ( this . flower . width == 'auto' || this . flower . width == undefined )
		this . flower . width = flower_img . get ( 0 ) . width ;
	if ( this . flower . height == 'auto' || this . flower . height == undefined )
		this . flower . height = flower_img . get ( 0 ) . height ;
	// end: preload images
	
	this . flowers = 0 ; // unblock will only occur if no flowers are left
	
	// block ui and show flower
	this . enable = function ()
	{
		this . flowers++ ;
		if ( $( 'body > div#' + this . container_id ) . length != 0 ) return ;
		
		var pos = this . base_obj . offset () ;
		
		var container = $( '<div></div>' ) ;
		container . attr ( 'id', this . container_id ) ;
		container . css ({
			position: 'absolute',
			'z-index': this . zindex,
			top: pos . top,
			left: 0,
			width: '100%',
			overflow: 'hidden',
			height: Math . max ( $( window ) . height (), $( 'body' ) . height () ) - pos . top
		}) ;
		if ( this . block_mode == 'base_obj' )
		{
			container . css ({
				left: pos . left,
				width: this . base_obj . width (),
				height: this . base_obj . height ()
			}) ;
		}
		container . hide () ;
		
		var background = $( '<div></div>' ) ;
		background . css ({
			position: 'absolute',
			width: '100%',
			height: '100%',
			background: this . background . css,
			opacity: this . background . opacity
		}) ;
		container . append ( background ) ;
		
		var bubble = $( '<div></div>' ) ;
		bubble . css ({
			position: 'relative',
			background: 'url("' + bubble_img . attr ( 'src' ) + '") no-repeat',
			width: this . bubble . width,
			height: this . bubble . height
		}) ;
		var flower = flower_img ;
		flower . css ({
			position: 'relative',
			left: this . bubble . width / 2 - this . flower . width / 2,
			top: this . bubble . height / 2 - this . flower . height / 2 - 2
		}) ;
		flower . mousedown ( function () { return false ; } ) ;
		bubble . append ( flower ) ;
		container . append ( bubble ) ;
		
		$( 'body' ) . append ( container ) ;
		
		bubble . css ( 'left', container . width () / 2 - this . bubble . width / 2 ) ;
		bubble . css ( 'top', container . height () / 2 - this . bubble . height / 2 ) ;
		
		if ( container . pngFix )
		  container . pngFix () ;
		
		container . mousedown ( function () { return false ; } ) ;
		container . fadeIn ( this . fade_speed ) ;
	}
	
	// unblock ui
	this . disable = function ()
	{
		if ( $( 'body > div#' + this . container_id ) . length == 0 ) return ;
		this . flowers = Math . max ( 0, this . flowers - 1 ) ;
		if ( this . flowers > 0 ) return ;
		
		$( 'body > div#' + this . container_id ) . fadeOut ( this . fade_speed, function () {
			$( this ) . remove () ;
		} ) ;
	}
	
}
