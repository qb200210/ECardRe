    	
// These two lines are required to initialize Express in Cloud Code.
var express = require('express');
var parseExpressHttpsRedirect = require('parse-express-https-redirect');
var parseExpressCookieSession = require('parse-express-cookie-session');
var app = express();
var sess;
Parse.initialize("eXr5eE3ff6vTMkTqsWe373eVZbuOLtafn7mFwlI2","5mX4KetLYXXusfdk6nObvgi615o3FghX1eXq9PXW");

// Global app configuration section
app.set('views', 'cloud/views');  // Specify the folder to find templates
app.set('view engine', 'ejs');    // Set the template engine
app.use(parseExpressHttpsRedirect());  // Require user to be on HTTPS.
app.use(express.bodyParser());
app.use(express.cookieParser('ECARD_IS_A_START'));
app.use(express.cookieSession({
	secret: 'we will succeed',
	cookie: { httpOnly: true }
}));
app.use(express.csrf());
app.use(function(req, res, next) {
	// Custom middleware for making csrf token available in EJS templates
	res.locals.csrf_field = req.session._csrf;
	sess=req.session;
	// define session variable
	sess.id;
	sess.userinfoObjs; // session copy of notes belonging to current user
	next();
});
app.use(parseExpressCookieSession({ cookie: { maxAge: 365 * 24 * 60 * 60 * 1000 } }));
// remember cookie for a month

// This is an example of hooking up a request handler with a specific request
// path and HTTP verb using the Express routing API.
app.get('/search', function(req, res) {
	var query = require('url').parse(req.url,true).query;
	sess.id = query.id;
	
	var ecardInfoClass = Parse.Object.extend("ECardInfo");
	var query = new Parse.Query(ecardInfoClass);
	query.get(sess.id, {
		success: function(object) {
			var collectedData = { 
				ecardId: sess.id,
				firstName: (typeof object.get("firstName") === 'undefined') ? "Mysterious user X" : object.get("firstName"),
				lastName: (typeof object.get("lastName") === 'undefined') ? "" : object.get("lastName"),
				company: (typeof object.get("company") === 'undefined') ? "Mysterious Company" : object.get("company"),
				title: (typeof object.get("title") === 'undefined') ? "Mysterious Position" : object.get("title"),
				city: (typeof object.get("city") === 'undefined') ? "Somewhere on Earth" : object.get("city"),
				portrait_url: object.get("portrait").url(),
			};
			if(Parse.User.current()) {
				// If the user has already login, don't offer login/signup
				res.render('ecardloggedin.ejs', collectedData);
			} else {
				// If the user has not login, show login/signup as well
				res.render('ecardgrow.ejs', collectedData);
			}
		},
		error: function(error) {
			// If the ecard is not found, bring user to login/signup page
			sess.id = '';
			res.redirect('/');
		}
	});			
});

app.get('/design', function(req, res){
	if(Parse.User.current()){
		// if the user has logged in, show design board
		Parse.User.current().fetch().then(function(user){
			var ecardInfoClass = Parse.Object.extend("ECardInfo");
			var query = new Parse.Query(ecardInfoClass);
			query.get(user.get("ecardId"), {
				success: function(object) {
					// display the current user's ecard
					
					res.render('editmycard.ejs', { 
						firstName: (typeof object.get("firstName") === 'undefined') ? "" : object.get("firstName"),
						lastName: (typeof object.get("lastName") === 'undefined') ? "" : object.get("lastName"),
						company: (typeof object.get("company") === 'undefined') ? "" : object.get("company"),
						title: (typeof object.get("title") === 'undefined') ? "" : object.get("title"),
						city: (typeof object.get("city") === 'undefined') ? "" : object.get("city"),
						about: object.get("about"),
						phone: object.get("phone"),
						message: object.get("message"),
						email: object.get("email"),
						linkedin: object.get("linkedin"),
						facebook: object.get("facebook"),
						twitter: object.get("twitter"),
						googleplus: object.get("googleplus"),
						web: object.get("web"),
						portrait_url: object.get("portrait").url(),
					});
				},
				error: function(error) {
					console.log('error finding my ecardinfo')
				}
			});	
		}, function(error){
			
		});
	} else{
		res.redirect('/');
	}
});

app.get('/note', function(req, res){
	var query = require('url').parse(req.url,true).query;
	var noteId = query.id;
	if(Parse.User.current()){
		// if the user has logged in, show design board
		
		var ecardNoteClass = Parse.Object.extend("ECardNote");
		var queryNote = new Parse.Query(ecardNoteClass);
		queryNote.get(noteId, {
			success: function(noteObj) {
				var ecardInfoClass = Parse.Object.extend("ECardInfo");
				var queryInfo = new Parse.Query(ecardInfoClass);
				queryInfo.get(noteObj.get("ecardId"),{
					success: function(infoObj){
						var updatedAt = noteObj.updatedAt.toString();
						splitDate = updatedAt.split(" ");
						updatedAt= splitDate[1] + " "+ splitDate[2] + " " + splitDate[3];
						var whenMet = noteObj.createdAt.toString();
						splitDate = whenMet.split(" ");
						whenMet= splitDate[1] + " "+ splitDate[2] + " " + splitDate[3];
						res.render('notedetails.ejs', { 
							noteId: noteId,
							firstName: (typeof infoObj.get("firstName") === 'undefined') ? "Mysterious User X" : infoObj.get("firstName"),
							lastName: (typeof infoObj.get("lastName") === 'undefined') ? "" : infoObj.get("lastName"),
							company: (typeof infoObj.get("company") === 'undefined') ? "Mysterious Company" : infoObj.get("company"),
							title: (typeof infoObj.get("title") === 'undefined') ? "Mysterious Position" : infoObj.get("title"),
							city: (typeof infoObj.get("city") === 'undefined') ? "Somewhere on Earth" : infoObj.get("city"),
							about: infoObj.get("about"),
							phone: infoObj.get("phone"),
							message: infoObj.get("message"),
							email: infoObj.get("email"),
							linkedin: infoObj.get("linkedin"),
							facebook: infoObj.get("facebook"),
							twitter: infoObj.get("twitter"),
							googleplus: infoObj.get("googleplus"),
							web: infoObj.get("web"),
							portrait_url: infoObj.get("portrait").url(),
							updatedAt: updatedAt,
							whenMet: whenMet,
							whereMet: (typeof noteObj.get("event_met") === 'undefined') ? "Mysterious Place" : noteObj.get("where_met"),
							eventMet: (typeof noteObj.get("event_met") === 'undefined') ? "Mysterious Event" : noteObj.get("event_met"),
							notes: (typeof noteObj.get("notes") === 'undefined') ? "" : noteObj.get("notes"),
						});
					},
					error: function(error){
						console.log(error)
					}
				})		
				
			},
			error: function(error) {
				console.log(error)
			}
		});	
		
	} else{
		res.redirect('/');
	}
});

app.post('/savenote', function(req, res){
	
	Parse.User.current().fetch().then(function(user){
		var ecardNoteClass = Parse.Object.extend("ECardNote");
		var query = new Parse.Query(ecardNoteClass);
		query.get(req.body.noteId, {
			success: function(object) {
				object.set("where_met", req.body.whereMet);
				object.set("event_met", req.body.eventMet);
				object.set("notes", req.body.notes);
				object.save(null, {								
					success: function(list) {
						// upon completion, return results
						res.json({successful : true});
					},
					error: function(error) {
					  // An error occurred while saving one of the objects.
					  res.json({successful : false});
					},
				});						
			},
			error: function(error) {
				res.json({successful : false});
			}
		});	
	}, function(error){
		res.json({successful : false});
	});	
	
});

app.get('/notif', function(req, res){
	if(Parse.User.current()){
		console.log('checking notifications!');
		Parse.User.current().fetch().then(function(user){
			var convClass = Parse.Object.extend("Conversations");
			var query = new Parse.Query(convClass);
			query.equalTo("partyB", user.get("ecardId"));
			query.notEqualTo("isDeleted", true);
			query.find({
				success: function(resultsConv) {
					console.log('found conversations!  ' + resultsConv.length);
					if(resultsConv.length != 0){
						// found notifications
						var ecardIds = [];
						for(var i=0 ; i< resultsConv.length ; i++) {
							ecardIds[i] = resultsConv[i].get("partyA");
						}
						var ecardInfoClass = Parse.Object.extend("ECardInfo");
						var query1 = new Parse.Query(ecardInfoClass);
						query1.containedIn("objectId", ecardIds); // find all collected ecards
						query1.find({
							success: function(resultsCards) {					
								// console.log('found resultsCards!  ' + resultsCards.length);					
								if(resultsCards.length != 0){
									resultsCards; // save it so can be accessed inside function calls									
									// sort found ecards, so that the order is consistent with that of found notes
									console.log('about to sort found cards/conv');
									var userinfoObjs = [];
									var tasksToDo = resultsConv.length;
									resultsConv.forEach(function(convobj) {
										
									  resultsCards.forEach(function(rstcard) {
										  
										  if(convobj.get("partyA") === rstcard.id){
											  tasksToDo = tasksToDo - 1;
											  // console.log('match: '+ rstcard.id + ' this is '+ tasksToDo); 
											  var whenMet = rstcard.createdAt.toString();
											  splitDate = whenMet.split(" ");
											  whenMet= splitDate[1] + " "+ splitDate[2];
											  userinfoObjs[tasksToDo] = {
															cardId : rstcard.id,
															firstName: rstcard.get("firstName"), 
															lastName: rstcard.get("lastName"),
															company: rstcard.get("company"),
															title: rstcard.get("title"),
															city: rstcard.get("city"),																	
															portrait_url: rstcard.get("portrait").url(),
															whenmet: whenMet
														}
											  
											  if(tasksToDo === 0) {
												  // the problem with 500 error is the below line, not because of rendering size limit
												  // sess.userinfoObjs = userinfoObjs; // save the search results to session
												  console.log('rendering conv...');
												  res.render('conversations.ejs', {convobjs : userinfoObjs, numConvs : resultsConv.length});														  
											  }
										  }
									  })
									});
								} else {
									// If 
									console.log('Ecard not found while conversations are non-empty');
									res.render('conversations.ejs', {});
								}
							},
							error: function(error) {
								// If the ecard is not found, bring user to login/signup page
								console.log(error);
								res.redirect('/');
							}
						});	
					} else{
						// no notifications
						console.log('no conversations found');
						var userinfoObjs = [];
						userinfoObjs[0] = {
											cardId : '',
											portrait_url: '',
											firstName: '',
											lastName: '',
											company: '',
											title: '',
											city: '',
										}
						res.render('conversations.ejs', {convobjs : userinfoObjs, numConvs : 0});				
					}
				},
				error: function(error) {
					console.log('error finding my notes')
				}
			});
			
		}, function(error){
			
		});
	} else{
		res.redirect('/');
	}
	
});

app.get('/searchcards', function(req, res){
	if(Parse.User.current()){
		// if the user has logged in, execute the search
		
		// if the notes/ ecards objects are not ready for some reason, need to pull again
		console.log('pulling for notes and ecards happening!');
		Parse.User.current().fetch().then(function(user){
			var ecardNoteClass = Parse.Object.extend("ECardNote");
			var query = new Parse.Query(ecardNoteClass);
			query.equalTo("userId", user.id);
			query.notEqualTo("isDeleted", true);
			// Initial search that will pull all notes belonging to current user 
			query.find({
				success: function(resultsNotes) {
					// somehow, if sess.noteobjs = resultsNotes, weird 500 error
					console.log('found resultsNotes!  ' + resultsNotes.length);
					if(resultsNotes.length != 0){
						// results will be all the ecardNotes belonging to current user 
						var ecardIds = [];
						for(var i=0 ; i< resultsNotes.length ; i++) {
							ecardIds[i] = resultsNotes[i].get("ecardId");
						}
						
						var ecardInfoClass = Parse.Object.extend("ECardInfo");
						var query1 = new Parse.Query(ecardInfoClass);
						query1.containedIn("objectId", ecardIds); // find all collected ecards
						query1.find({
							success: function(resultsCards) {
					
								// console.log('found resultsCards!  ' + resultsCards.length);
					
								if(resultsCards.length != 0){
									resultsCards; // save it so can be accessed inside function calls
									if(resultsCards.length != resultsNotes.length){
										// need to check if the number of notes equals to number of found cards -- if not, need to fix db
										res.send('inconsistency between no. of notes and no. of corresponding ecards.');
									} else{
										// sort found ecards, so that the order is consistent with that of found notes
										console.log('about to sort found cards/notes');
										var userinfoObjs = [];
										var tasksToDo = resultsCards.length;
										resultsNotes.forEach(function(noteobj) {
											
										  resultsCards.forEach(function(rstcard) {
											  
											  if(noteobj.get("ecardId") === rstcard.id){
												  tasksToDo = tasksToDo - 1;
												  // console.log('match: '+ rstcard.id + ' this is '+ tasksToDo); 
												  userinfoObjs[tasksToDo] = {
													            noteId : noteobj.id,
																cardId : rstcard.id,
																firstName: rstcard.get("firstName"), 
																lastName: rstcard.get("lastName"),
																company: rstcard.get("company"),
																title: rstcard.get("title"),
																city: rstcard.get("city"),																	
																portrait_url: rstcard.get("portrait").url(),
																wheremet: noteobj.get("where_met"),
																eventmet: noteobj.get("event_met"),
															}
												  
												  if(tasksToDo === 0) {
													  // the problem with 500 error is the below line, not because of rendering size limit
													  // sess.userinfoObjs = userinfoObjs; // save the search results to session
													  res.render('searchresults.ejs', {usrobjs : userinfoObjs, numNotes : resultsCards.length});														  
												  }
											  }
										  })
										});
										
										
									}
								} else {
									// If 
									console.log('Ecard not found while notes are non-empty');
									res.render('searchresults', {});
								}
							},
							error: function(error) {
								// If the ecard is not found, bring user to login/signup page
								console.log(error);
								res.redirect('/');
							}
						});	
					} else {
						console.log('no note found');
						var userinfoObjs = [];
						userinfoObjs[0] = {
											noteId : '',
											cardId : '',
											portrait_url: '',
											firstName: '',
											lastName: '',
											company: '',
											title: '',
											city: '',
											wheremet: '',
											eventmet: '',
										}
						res.render('searchresults.ejs', {usrobjs : userinfoObjs, numNotes : 0});							
					} 
				},
				error: function(error) {
					console.log('error finding my notes')
				}
			});	
		}, function(error){
			
		});
		
	} else{
		res.redirect('/');
	}
});

app.post('/savedesign', function(req, res){
	Parse.User.current().fetch().then(function(user){
		var ecardInfoClass = Parse.Object.extend("ECardInfo");
		var query = new Parse.Query(ecardInfoClass);
		query.get(user.get("ecardId"), {
			success: function(object) {
				// split the input full name into first/last name
				splitName = req.body.name.split(" ");
				firstName = "";
				lastName = "";
				for(var i=0; i< splitName.length-2; i++){
					firstName = firstName + splitName[i] + " ";
				}
				for(var i=splitName.length-2; i< splitName.length-1; i++){
					firstName = firstName + splitName[i];
				}
				if(splitName.length>0){
					lastName = splitName[splitName.length-1];
				}
				if(firstName != ""){
					object.set("firstName", firstName);
				} else {
					object.unset("firstName");
				}
				if(lastName != ""){
					object.set("lastName", lastName);
				} else {						
					object.unset("lastName");
				}
				// the rest of main card
				if(req.body.title != ""){
					object.set("title", req.body.title);
				}
				if(req.body.company != ""){
					object.set("company", req.body.company);
				}
				if(req.body.city != ""){
					object.set("city", req.body.city);
				}
				// put extra info
				if(req.body.about != ""){
					object.set("about", req.body.about);
				} else {
					object.unset("about");
				}
				if(req.body.linkedin != ""){
					object.set("linkedin", req.body.linkedin);
				} else {
					object.unset("linkedin");
				}
				if(req.body.phone != ""){
					object.set("phone", req.body.phone);
				} else {
					object.unset("phone");
				}
				if(req.body.message != ""){
					object.set("message", req.body.message);
				} else {
					object.unset("message");
				}
				if(req.body.email != ""){
					object.set("email", req.body.email);
				} else {
					object.unset("email");
				}
				if(req.body.facebook != ""){
					object.set("facebook", req.body.facebook);
				} else {
					object.unset("facebook");
				}
				if(req.body.twitter != ""){
					object.set("twitter", req.body.twitter);
				} else {
					object.unset("twitter");
				}
				if(req.body.googleplus != ""){
					object.set("googleplus", req.body.googleplus);
				} else {
					object.unset("googleplus");
				}
				if(req.body.web != ""){
					object.set("web", req.body.web);
				} else {
					object.unset("web");
				}
				if(req.body.img_url != ""){
					var Image = require("parse-image");
					Parse.Cloud.httpRequest({
						url: req.body.img_url	 
					  }).then(function(response) {						
						var image = new Image();
						return image.setData(response.buffer);		 
					  }).then(function(image) {
						// Crop the image to the smaller of width or height.
						width = Math.min(image.width(), image.width()* req.body.perc_width);
						height = Math.min(image.height(), image.height()* req.body.perc_height);
						left = Math.max(0, image.width()* req.body.perc_left);
						top = Math.max(0,image.height()* req.body.perc_top);
						return image.crop({
						  left: left,
						  top: top,
						  width: width,
						  height: height
						});
					 
					  }).then(function(image) {
						// Resize the image to 64x64.
						return image.scale({
						  width: 64,
						  height: 64
						});
					 
					  }).then(function(image) {
						// Make sure it's a JPEG to save disk space and bandwidth.
						return image.setFormat("PNG");
					 
					  }).then(function(image) {
						// Get the image data in a Buffer.
						return image.data();
					 
					  }).then(function(buffer) {
						// Save the image into a new file.
						var base64 = buffer.toString("base64");
						var cropped = new Parse.File("newPortrait.jpg", { base64: base64 });
						return cropped.save();
					 
					  }).then(function(cropped) {
						// Attach the image file to the original object.
						object.set("portrait", cropped);
						
						// save changes
						object.save(null, {
							
							success: function(list) {
								// upon completion, go back to homepage
								res.json({successful : true});
							},
							error: function(error) {
							  // An error occurred while saving one of the objects.
							  res.json({successful : false});
							},
						});		
					}, function(error) {
					  // The file either could not be read, or could not be saved to Parse.
					  res.json({successful : false});
					});
				} else {
					// if portrait not updated, directly upload
					// save changes
					object.save(null, {
						
						success: function(list) {
							// upon completion, go back to homepage
							res.json({successful : true});
						},
						error: function(error) {
						  // An error occurred while saving one of the objects.
						  res.json({successful : false});
						},
					});		
					
				}
						
			},
			error: function(error) {
				console.log('error finding my ecardinfo')
				res.json({successful : false});
			}
		});	
	}, function(error){
		res.json({successful : false});
	});
});

app.post('/save', function(req, res){
	// This is to respond to user's save Ecard action
	if(req.body.saveordiscard === '0'){
		Parse.User.current().fetch().then(function(user){
			var ecardNoteClass = Parse.Object.extend("ECardNote");
			var query = new Parse.Query(ecardNoteClass);
			query.equalTo("userId", user.id);
			console.log(req.body.ecardId);
			if(req.body.ecardId != "" && !(typeof req.body.ecardId === 'undefined') ){
				// if this is a post carrying ecardId, use it
				query.equalTo("ecardId", req.body.ecardId);
				ecardId = req.body.ecardId;
			} else {
				query.equalTo("ecardId", sess.id);
				ecardId = sess.id;
			}
			query.find({
				success: function(results) {
					if(results.length === 0){
						// This card has not been collected
						var ecardInfoClass = Parse.Object.extend("ECardInfo");
						var query1 = new Parse.Query(ecardInfoClass);
						query1.get(ecardId, {
							success: function(object) {		
								console.log('Ecard found');		
								var noteObject = new Parse.Object('ECardNote');
								var usrACL = new Parse.ACL(Parse.User.current());
								usrACL.setPublicReadAccess(false);
								usrACL.setPublicWriteAccess(false);
								noteObject.setACL(usrACL);
								noteObject.save({userId: Parse.User.current().id, ecardId: object.id, EcardUpdatedAt: object.updatedAt }, {
									success: function(){
										console.log('Save note successful');
										sess.id='';
										// sess.userinfoObjs = []; // clear the search results to reflect this change
										res.json({status : 0});
									},
									error: function(error){
										console.log('Save note fails');
										res.json({status : 9});
									}
								});
							},
							error: function(error) {
								// If the ecard is not found, bring user to login/signup page
								console.log('Ecard not found');
								res.json({status : 9});
							}
						});	
					} else {
						var foundNoteObj = results[0];
						if(foundNoteObj.get("isDeleted") == false || foundNoteObj.get("isDeleted") == "" || (typeof foundNoteObj.get("isDeleted") === 'undefined')) {
							console.log('ecard already collected');
							sess.id='';
							res.json({status : 2});
						} else {
							// if the note existed but deleted, flip the flag
							foundNoteObj.set("isDeleted", false);
							foundNoteObj.save({}, {
								success: function(){
									sess.id='';
									console.log('note revived');
									res.json({status : 0});
								},
								error: function(error){
									console.log('Save note fails');
									res.json({status : 9});
								}
							});
						}
					} 
				},
				error: function(error) {
					console.log('error finding my ecardinfo');
					res.json({status : 9});
				}
			});	
		}, function(error){
			
		});
	} else {
		// discard
		sess.id='';
		console.log('discarded ecard');
		res.json({status : 3});
	}
});

app.post('/login', function(req, res){
	Parse.User.logIn(req.body.email, req.body.password).then(function(){
		// Login successful, redirect to homepage, where dashboard will be shown
		// here forces to use email as login name
		console.log('log in successful')
		console.log(sess.id);
		// if remember me is checked, set 1 year free of logIn
		// if(req.body.remember === "remember-me") {
			// console.log('remember me');
			// //sess.cookie.maxAge = 365 * 24 * 60 * 60 * 1000;
		// } else {
			// // This user should log in again after restarting the browser
			// console.log('forget me');
		// }
		
		if(!(typeof sess.id === 'undefined') && sess.id != '') {
			console.log('An Ecard to be collected');
			// If there is an ecard to be collected from the url, display the page and offer to save
			var ecardInfoClass = Parse.Object.extend("ECardInfo");
			var query = new Parse.Query(ecardInfoClass);
			query.get(sess.id, {
				success: function(object) {
					var collectedData = { 
						ecardId: sess.id,
						firstName: (typeof object.get("firstName") === 'undefined') ? "Mysterious user X" : object.get("firstName"),
						lastName: (typeof object.get("lastName") === 'undefined') ? "" : object.get("lastName"),
						company: (typeof object.get("company") === 'undefined') ? "Mysterious Company" : object.get("company"),
						title: (typeof object.get("title") === 'undefined') ? "Mysterious Position" : object.get("title"),
						city: (typeof object.get("city") === 'undefined') ? "Somewhere on Earth" : object.get("city"),
						portrait_url: object.get("portrait").url(),
					};					
					// The user has already login, don't offer login/signup
					res.render('ecardloggedin.ejs', collectedData);					
				},
				error: function(error) {
					// If the ecard is not found, bring user to login/signup page
					res.redirect('/');
				}
			});						
		} else{
			// if no ecard to collect, redirect to dashboard
			res.redirect('/');
		}
	}, 
	function(error){
		// Login fails, redirect to homepage, where login/signup page is shown	
		res.render('failedloginpage',{code: error.code, msg: error.message, name: req.body.name});
	})
})

app.post('/linkedinSignin', function(req, res){
	if(Parse.User.current()){
		// this is necessary, otherwise signup while a session is on-going will fail
		Parse.User.logOut();
	}
	var user = new Parse.User();
	splitStr = req.body.linkedinObj.split(/,,/);
	firstName = splitStr[0];
	lastName = splitStr[1];
	emailAddress = splitStr[2];
	pictureUrl = splitStr[3];
	cityStr = splitStr[4];
	companyStr = splitStr[5];
	titleStr = splitStr[6];
	linkedinID = splitStr[7];
	console.log(linkedinID);

	var password = linkedinID + "jsdj32RIfd28UFaf2";

	console.log(pictureUrl);
	usrName = firstName+lastName+linkedinID;
	user.set("username", usrName); // email==username
	user.set("name", firstName + " " + lastName);
	user.set("password", password);
	user.set("email", emailAddress);
	// the ACL must be set over here, instead of on current()
	var usrACL = new Parse.ACL(Parse.User.current());
	usrACL.setPublicReadAccess(false);
	usrACL.setPublicWriteAccess(false);
	user.setACL(usrACL);
	user.signUp(null).then(function(currentUser){
		// Signup successful, redirect to homepage, where dashboard will be shown
		console.log('sign up successful')

		// Parse.User.requestPasswordReset(currentUser.getEmail());
		// Upon sign up, create the user's ECard with basically no information
		var infoObject = new Parse.Object('ECardInfo');
		var usrACL = new Parse.ACL(Parse.User.current());
		usrACL.setPublicReadAccess(true);
		usrACL.setPublicWriteAccess(false);
		infoObject.setACL(usrACL);
		// set name 
		if(firstName != ""){
			infoObject.set("firstName", firstName);
		} else {
			infoObject.unset("firstName");
		}
		if(lastName != ""){
			infoObject.set("lastName", lastName);
		} else {						
			infoObject.unset("lastName");
		}
		if( companyStr != ""){
			infoObject.set("company", companyStr);
		} else {						
			infoObject.unset("company");
		}
		if(cityStr != ""){
			infoObject.set("city", cityStr);
		} else {						
			infoObject.unset("city");
		}
		if(titleStr != ""){
			infoObject.set("title", titleStr);
		} else {						
			infoObject.unset("title");
		}
		
		// initiate default profile portrait
		var Image = require("parse-image");
		Parse.Cloud.httpRequest({
			//url: "http://www.micklestudios.com/assets/img/emptyprofile.png"
			url: pictureUrl		 
		  }).then(function(response) {
			var image = new Image();
			return image.setData(response.buffer);		 
		  }).then(function(image) {
			// Crop the image to the smaller of width or height.
			var size = Math.min(image.width(), image.height());
			return image.crop({
			  left: (image.width() - size) / 2,
			  top: (image.height() - size) / 2,
			  width: size,
			  height: size
			});
		 
		  }).then(function(image) {
			// Resize the image to 64x64.
			return image.scale({
			  width: 64,
			  height: 64
			});
		 
		  }).then(function(image) {
			// Make sure it's a JPEG to save disk space and bandwidth.
			return image.setFormat("PNG");
		 
		  }).then(function(image) {
			// Get the image data in a Buffer.
			return image.data();
		 
		  }).then(function(buffer) {
			// Save the image into a new file.
			var base64 = buffer.toString("base64");
			var cropped = new Parse.File("emptyPortrait.jpg", { base64: base64 });
			return cropped.save();
		 
		  }).then(function(cropped) {
			// Attach the image file to the original object.
			infoObject.save({portrait : cropped}, {
				success: function(){
					console.log('save info successful');
					// somwhow currentUser cannot be used as function(currentUser) or the actual value is not passed down
					currentUser.save({ecardId : infoObject.id});
					console.log(sess.id);
					if(!(typeof sess.id === 'undefined') && sess.id != '') {
						console.log('An Ecard to be collected');
						// If there is an ecard to be collected from the url, display the page and offer to save
						var ecardInfoClass = Parse.Object.extend("ECardInfo");
						var query = new Parse.Query(ecardInfoClass);
						query.get(sess.id, {
							success: function(object) {
								var collectedData = { 
									firstName: (typeof object.get("firstName") === 'undefined') ? "Mysterious user X" : object.get("firstName"),
									lastName: (typeof object.get("lastName") === 'undefined') ? "" : object.get("lastName"),
									company: (typeof object.get("company") === 'undefined') ? "Mysterious Company" : object.get("company"),
									title: (typeof object.get("title") === 'undefined') ? "Mysterious Position" : object.get("title"),
									city: (typeof object.get("city") === 'undefined') ? "Somewhere on Earth" : object.get("city"),
									portrait_url: object.get("portrait").url(),
								};					
								// The user has already login, don't offer login/signup
								console.log("before rendering");
								res.render('ecardloggedin.ejs', collectedData);					
							},
							error: function(error) {
								// If the ecard is not found, bring user to login/signup page
								res.redirect('/');
							}
						});						
					} else{
						// this redirect statement must be included here, as save() is non-blocking
						// if not placed here, redirect will happen before save is complete
						// if no ecard to collect, redirect to dashboard
						res.redirect('/');
					}
				},
				error: function(error){
					console.log('save info fails')		
					res.redirect('/');				
				}
			});
		  });
	}, 
	function(error){
		// Signup fails: there is already a existing record, then use login procedure
		Parse.User.logIn(usrName, password).then(function(){
			// Login successful, redirect to homepage, where dashboard will be shown
			// here forces to use email as login name
			console.log('log in successful')
			console.log(sess.id);
		
			if(!(typeof sess.id === 'undefined') && sess.id != '') {
				console.log('An Ecard to be collected');
				// If there is an ecard to be collected from the url, display the page and offer to save
				var ecardInfoClass = Parse.Object.extend("ECardInfo");
				var query = new Parse.Query(ecardInfoClass);
				query.get(sess.id, {
					success: function(object) {
						var collectedData = { 
							ecardId: sess.id,
							firstName: (typeof object.get("firstName") === 'undefined') ? "Mysterious user X" : object.get("firstName"),
							lastName: (typeof object.get("lastName") === 'undefined') ? "" : object.get("lastName"),
							company: (typeof object.get("company") === 'undefined') ? "Mysterious Company" : object.get("company"),
							title: (typeof object.get("title") === 'undefined') ? "Mysterious Position" : object.get("title"),
							city: (typeof object.get("city") === 'undefined') ? "Somewhere on Earth" : object.get("city"),
							portrait_url: object.get("portrait").url(),
							};					
						// The user has already login, don't offer login/signup
						res.render('ecardloggedin.ejs', collectedData);				
						},
					error: function(error) {
						// If the ecard is not found, bring user to login/signup page
						res.redirect('/');
						}
					});						
			} else{
				// if no ecard to collect, redirect to dashboard
				res.redirect('/');
			}
		}, 
		function(error){
			// Login fails, redirect to homepage, where login/signup page is shown	
			res.render('failedloginpage',{code: error.code, msg: error.message, name: req.body.name});
		})
	})
})


app.post('/signup', function(req, res){
	if(Parse.User.current()){
		// this is necessary, otherwise signup while a session is on-going will fail
		Parse.User.logOut();
	}
	var password = "jsdj32RIfd28UFaf2";
	var user = new Parse.User();
	user.set("username", req.body.email); // email==username
	user.set("name", req.body.name);
	user.set("password", password);
	user.set("email", req.body.email);
	// the ACL must be set over here, instead of on current()
	var usrACL = new Parse.ACL(Parse.User.current());
	usrACL.setPublicReadAccess(false);
	usrACL.setPublicWriteAccess(false);
	user.setACL(usrACL);
	user.signUp(null).then(function(currentUser){
		// Signup successful, redirect to homepage, where dashboard will be shown
		console.log('sign up successful')

		// Parse.User.requestPasswordReset(currentUser.getEmail());
		// Upon sign up, create the user's ECard with basically no information
		var infoObject = new Parse.Object('ECardInfo');
		var usrACL = new Parse.ACL(Parse.User.current());
		usrACL.setPublicReadAccess(true);
		usrACL.setPublicWriteAccess(false);
		infoObject.setACL(usrACL);
		// set name 
		splitName = req.body.name.split(" ");
		firstName = "";
		lastName = "";
		for(var i=0; i< splitName.length-1; i++){
			firstName = firstName + splitName[i] + " ";
		}
		if(splitName.length>0){
			lastName = splitName[splitName.length-1];
		}
		if(firstName != ""){
			infoObject.set("firstName", firstName);
		} else {
			infoObject.unset("firstName");
		}
		if(lastName != ""){
			infoObject.set("lastName", lastName);
		} else {						
			infoObject.unset("lastName");
		}
		// initiate default profile portrait
		var Image = require("parse-image");
		Parse.Cloud.httpRequest({
			url: "http://www.micklestudios.com/assets/img/emptyprofile.png"		 
		  }).then(function(response) {
			
			var image = new Image();
			return image.setData(response.buffer);		 
		  }).then(function(image) {
			// Crop the image to the smaller of width or height.
			var size = Math.min(image.width(), image.height());
			return image.crop({
			  left: (image.width() - size) / 2,
			  top: (image.height() - size) / 2,
			  width: size,
			  height: size
			});
		 
		  }).then(function(image) {
			// Resize the image to 64x64.
			return image.scale({
			  width: 64,
			  height: 64
			});
		 
		  }).then(function(image) {
			// Make sure it's a JPEG to save disk space and bandwidth.
			return image.setFormat("PNG");
		 
		  }).then(function(image) {
			// Get the image data in a Buffer.
			return image.data();
		 
		  }).then(function(buffer) {
			// Save the image into a new file.
			var base64 = buffer.toString("base64");
			var cropped = new Parse.File("emptyPortrait.jpg", { base64: base64 });
			return cropped.save();
		 
		  }).then(function(cropped) {
			// Attach the image file to the original object.
			infoObject.save({portrait : cropped}, {
				success: function(){
					console.log('save info successful');
					// somwhow currentUser cannot be used as function(currentUser) or the actual value is not passed down
					currentUser.save({ecardId : infoObject.id});
					console.log(sess.id);
					if(!(typeof sess.id === 'undefined') && sess.id != '') {
						console.log('An Ecard to be collected');
						// If there is an ecard to be collected from the url, display the page and offer to save
						var ecardInfoClass = Parse.Object.extend("ECardInfo");
						var query = new Parse.Query(ecardInfoClass);
						query.get(sess.id, {
							success: function(object) {
								var collectedData = { 
									firstName: (typeof object.get("firstName") === 'undefined') ? "Mysterious user X" : object.get("firstName"),
									lastName: (typeof object.get("lastName") === 'undefined') ? "" : object.get("lastName"),
									company: (typeof object.get("company") === 'undefined') ? "Mysterious Company" : object.get("company"),
									title: (typeof object.get("title") === 'undefined') ? "Mysterious Position" : object.get("title"),
									city: (typeof object.get("city") === 'undefined') ? "Somewhere on Earth" : object.get("city"),
									portrait_url: object.get("portrait").url(),
								};					
								// The user has already login, don't offer login/signup
								res.render('ecardloggedin.ejs', collectedData);					
							},
							error: function(error) {
								// If the ecard is not found, bring user to login/signup page
								res.redirect('/');
							}
						});						
					} else{
						// this redirect statement must be included here, as save() is non-blocking
						// if not placed here, redirect will happen before save is complete
						// if no ecard to collect, redirect to dashboard
						res.redirect('/');
					}
				},
				error: function(error){
					console.log('save info fails')		
					res.redirect('/');				
				}
			});
		  });
	}, 
	function(error){
		// Signup fails, redirect to homepage, where login/signup page is shown
		res.render('failedloginpage',{code: error.code, msg: error.message, name: req.body.name});
	})
})

app.get('/logout', function(req, res){
	Parse.User.logOut();
	sess.userinfoObjs = []; // clean the search results from session upon exit
	res.redirect('dummypage');
})

app.get('/', function(req, res){
	if(Parse.User.current()){
		// if the user has logged in, show dashboard
		
		// Must use fetch(), previously tried current().get("name"), returns null
		Parse.User.current().fetch().then(function(user){
			var ecardInfoClass = Parse.Object.extend("ECardInfo");
			var query = new Parse.Query(ecardInfoClass);
			query.get(user.get("ecardId"), {
				success: function(object) {
					// display the current user's ecard
					res.render('dashboard.ejs', { 
						ecardId: object.id,
						firstName: (typeof object.get("firstName") === 'undefined') ? "Mysterious user X" : object.get("firstName"),
						lastName: (typeof object.get("lastName") === 'undefined') ? "" : object.get("lastName"),
						company: (typeof object.get("company") === 'undefined') ? "Mysterious Company" : object.get("company"),
						title: (typeof object.get("title") === 'undefined') ? "Mysterious Position" : object.get("title"),
						city: (typeof object.get("city") === 'undefined') ? "Somewhere on Earth" : object.get("city"),
						username: user.get('username'),
						portrait_url: object.get("portrait").url(),
					 });				 
				},
				error: function(error) {
					console.log('error finding my ecardinfo')
				}
			});	
		}, function(error){
			
		});
		
	} else {
		// if the user hasn't logged in, redirect to login/signup page
		res.render('loginpage.ejs');
	}
})

// if the url points to unhandled pages, redirect to login/dashboard
app.get('*', function(req, res, next) {
  var err = new Error();
  err.status = 404;
  next(err);
});
 
// handling 404 errors
app.use(function(err, req, res, next) {
  if(err.status == 404) {
	  // if user session, redirect to dashboard
	  if(Parse.User.current()) {
		res.redirect('/');
	  } else{
		// if user hasn't login, redirect to login/signup page
		res.redirect('/');
		}
  } else {
    return next();
  }
	
});

var hasOwnProperty = Object.prototype.hasOwnProperty;

function isEmpty(obj) {

    // null and undefined are "empty"
    if (obj == null) return true;

    // Otherwise, does it have any properties of its own?
    // Note that this doesn't handle
    // toString and valueOf enumeration bugs in IE < 9
    for (var key in obj) {
        if (hasOwnProperty.call(obj, key)) return false;
    }

    return true;
}

// Attach the Express app to Cloud Code.
app.listen();
