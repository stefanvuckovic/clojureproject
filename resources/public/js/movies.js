$(document).ready(function() {
	$(document).on("keyup","#search", function(e) {
		if(e.keyCode == 13) {
	        var search = $("#search").val();
	        getMovies(1, search);
	    }
	});
     
	getMovies(1, null);
	
	
});

function getMovies(page, title) {
	var params = {};
	params['page'] = page;
	if(title != null){
		params['title'] = title;
	}
    $.get("/pagination", params,
    function(data) {
        $("#movies").html(data);
    });


}