  require('cloud/app.js');
// Use Parse.Cloud.define to define as many cloud functions as you want.
// For example:
Parse.Cloud.define("hello", function(request, response) {
  response.success("Hello world!");
});

Parse.Cloud.beforeSave("History", function(request, status) {
         var request_obj_id = request.object.get("objectId");
         var request_user_id = request.object.get("userId");
         var historyObject = Parse.Object.extend('History');
         var query = new Parse.Query(historyObject);
         query.equalTo("objectId", request_obj_id);
         query.find( {
	  success: function(results) {
			if (results.length > 0) {
				console.log("entry already existed");
				if (results[0].get("userId") ==  request_user_id )
				{
					console.log("userId does not change");
					status.success(" approve save");
				}
				else
				{
					console.log("Warning: userId was set to change")
					status.error(" Save operation is denied ");
				}
			}
			else{
				console.log("entry not existe");
				status.success("First time entry save, approve!");
			}
			},
	error:  function(){
				status.error("History Query Error");
		}
	});
});

Parse.Cloud.beforeSave("ECardNote", function(request, status) {
         var request_obj_id = request.object.get("objectId");
         var request_user_id = request.object.get("userId");
         var noteObject = Parse.Object.extend('ECardNote');
         var query = new Parse.Query(noteObject);
         query.equalTo("objectId", request_obj_id);
         query.find( {
	  success: function(results) {
			if (results.length > 0) {
				console.log("entry already existed");
				if (results[0].get("userId") ==  request_user_id )
				{
					console.log("userId does not change");
					status.success(" approve save");
				}
				else
				{
					console.log("Warning: userId was set to change")
					status.error(" Save operation is denied ");
				}
			}
			else{
				console.log("entry not existe");
				status.success("First time entry save, approve!");
			}
			},
	error:  function(){
				status.error("ECardNote Query Error");
		}
	});
});


Parse.Cloud.beforeSave("User", function(request, status) {
         var request_obj_id = request.object.get("objectId");
         var max_num_card = request.object.get("maxNumCard");
         var max_num_notes = request.object.get("maxNotes");
         var max_history = request.object.get("maxHistory");
         var max_conv = request.object.get("maxConv");
         var premium_field = request.object.get("Premium");
         var auth_data = request.object.get("authData");
         var email_verified = request.object.get("emailVerified");
         var ecard_id = request.object.get("ecardId");


         var noteObject = Parse.Object.extend('User');
         var query = new Parse.Query(noteObject);
         query.equalTo("objectId", request_obj_id);
         query.find( {
	  success: function(results) {
			if (results.length > 0) {
				console.log("entry already existed");
				if (results[0].get("ecardId") == ecard_id  &&
				    results[0].get("maxNumCard") == max_num_card && 
				    results[0].get("maxNotes") == max_notes && 
				    results[0].get("maxHistory") == max_history&& 
				    results[0].get("maxConv") == max_conv && 
				    results[0].get("Premium") == premium_field && 
				    results[0].get("authData") == auth_data && 
				    results[0].get("emailVerified") == email_verified
				  )
				{
					console.log("userId does not change");
					status.success(" approve save");
				}
				else
				{
					console.log("Warning: protected field was set to change")
					status.error(" Save operation is denied ");
				}
			}
			else{
				console.log("entry not existe");
				status.success("First time entry save, approve!");
			}
			},
	error:  function(){
				status.error("ECardNote Query Error");
		}
	});
});


Parse.Cloud.beforeSave("ECardInfo", function(request, status) {
         var request_obj_id = request.object.get("objectId");
         var request_user_id = request.object.get("userId");
         var infoObject = Parse.Object.extend('ECardInfo');
         var query = new Parse.Query(infoObject);
         query.equalTo("objectId", request_obj_id);
         query.find( {
	  success: function(results) {
			if (results.length > 0) {
				console.log("entry already existed");
				if (results[0].get("userId") ==  request_user_id )
				{
					console.log("userId does not change");
					status.success(" approve save");
				}
				else
				{
					console.log("Warning: userId was set to change")
					status.error(" Save operation is denied ");
				}
			}
			else{
				console.log("entry not existe");
				status.success("First time entry save, approve!");
			}
			},
	error:  function(){
				status.error("ECardInfo Query Error");
		}
	});
});

Parse.Cloud.beforeSave("ECardTemplate", function(request, status) {
  // Set up to modify user data
  var  company_str = request.object.get("companyName").replace(/^\s+|\s+$/g, '');
  var templateObject = Parse.Object.extend('ECardTemplate');
  var query = new Parse.Query(templateObject);
  query.equalTo("companyName", company_str);
  // couting
  var company_existed = 2;
  console.log("before count queries");
  query.count().then( function(count) {
			if (count > 0) {
				console.log("company already existed");
				company_existed = 1;
			}
			else{
				console.log("company not existe");
				company_existed = 0;
			}
			return company_existed;
			}). then(function(company_existed){
			     if (company_existed == 0) {
      				// need to add counter to enforce the quota of the google query
      				console.log("successfully retrieved company " + company_str);
      				var GOOGLE_API_KEY = "AIzaSyBZ38MmvTIjCWag2mTrcEypNVSBsPTNApE";
      				var GOOGLE_CSE_ID = "008409895350719005494:alajzsekfwc";
      				var search_str = company_str.replace(/[,\s]+/g, "+") + "+logo";
      				var google_api_url = "https://www.googleapis.com/customsearch/v1?key="+GOOGLE_API_KEY+"&cx=" + GOOGLE_CSE_ID + "&q=" + search_str + "&searchType=image&fileType=png&imgSize=large&alt=json";
     				 var logo_url = ""; 
      				var img_file_name = company_str.replace(/[,\s]+/g, "_") + ".png"
     				console.log(search_str);
				var Image = require("parse-image");
      				console.log(google_api_url);
      				Parse.Cloud.httpRequest({
  					url: google_api_url
				}).then(function(httpResponse) {
  					console.log(httpResponse.text);
					obj=JSON.parse(httpResponse.text);
					logo_url = obj.items[0].link;
					console.log(logo_url);
					Parse.Cloud.httpRequest({	
						url: logo_url
					}).then(function(nextHttpResponse){
					    	var image = new Image();
						console.log("here next http response");
						return image.setData(nextHttpResponse.buffer);	
					}).then(function(image){
						var size = Math.min(image.width(), image.height());
						var new_ratio = 64*1.0/size;
						// scale the image with respect to ratio
						console.log("scale the image with ratio " + new_ratio.toString());
						return image.scale({ ratio: new_ratio});
					}).then(function(image) {
						// Make sure it's a JPEG to save disk space and bandwidth.
						console.log("setting the image format");
						return image.setFormat("PNG");		 
		 			 }).then(function(image) {
						// Get the image data in a Buffer.
						return image.data();
		 			 }).then(function(buffer) {
						// Save the image into a new file.
						var base64 = buffer.toString("base64");
						var cropped = new Parse.File(img_file_name, { base64: base64 });
						console.log("image saving");
						return cropped.save();
					}).then(function(logoImage){
						console.log("before save " + logoImage);
						request.object.set("companyLogo", logoImage);
					}).then(function(result){
						console.log("Done with results " + result);
						status.success();
					}, function(nextHttpResponse){
						status.error("Error in HttpRes: " + nextHttpResponse.code + " " + nextHttpResponse.message );
					});
				}, 
 				function(error) {
    					// Set the job's error status
    					status.error("Error in Google Search: " + error.code + " " + error.message );
  				});
			}
			else {
				status.error("Do not save, since company already existed!");
			}//else
		}, function() {
			status.error("ECardTemplate Query Error");
		});
 });



Parse.Cloud.job("logoSearch", function(request, status) {
  // Set up to modify user data
  //Parse.Cloud.useMasterKey();
  var counter = 0;
  // Query for company logo
  //var EcardTemplateClass = Parse.Object.extend("ECardTemplate");
  var query = new Parse.Query("ECardTemplate");
  query.limit (100);
  query.equalTo("companyLogo", null);
  //query.equalTo("companyName", "NASA");

query.find(). then(function(list) {
      // need to add counter to enforce the quota of the google query
      console.log("successfully retrieved " + list.length + " entries" );
      var GOOGLE_API_KEY = "AIzaSyBZ38MmvTIjCWag2mTrcEypNVSBsPTNApE";
      var GOOGLE_CSE_ID = "008409895350719005494:alajzsekfwc";
      var object = list[0];
      var company_str = object.get("companyName");
      var search_str = company_str + "+logo";
      var google_api_url = "https://www.googleapis.com/customsearch/v1?key="+GOOGLE_API_KEY+"&cx=" + GOOGLE_CSE_ID + "&q=" + search_str + "&searchType=image&fileType=jpg&imgSize=small&alt=json";
      var logo_url = ""; 
      var img_file_name = company_str + ".png"
      console.log(search_str);
      console.log(google_api_url);
      Parse.Cloud.httpRequest({
  	url: google_api_url
	}).then(function(httpResponse) {
  		console.log(httpResponse.text);
		obj=JSON.parse(httpResponse.text);
		logo_url = obj.items[0].link;
		console.log(logo_url);
		Parse.Cloud.httpRequest({	
			url: logo_url
		}).then(function(nextHttpResponse){
			var logoFile = new Parse.File(img_file_name, {base64: nextHttpResponse.buffer.toString('base64', 0, httpResponse.buffer.length)});
			console.log("get logo");
			return logoFile.save();
		}).then(function(logoImage){
			console.log("before save " + logoImage);
			object.set("companyLogo", logoImage);
		                  return object.save();
		}).then(function(result){
			status.success("Done with results" + result);
		}, function(nextHttpResponse){
			status.error("Error: " + nextHttpResponse.code + " " + nextHttpResponse.message );
		});
		}, 
 		function(error) {
    			// Set the job's error status
    			status.error("Error: " + error.code + " " + error.message );
  		});
       },
       function(error) {
    			// Set the job's error status
    			status.error("Error: " + error.code + " " + error.message );
  		});
 });