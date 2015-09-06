    	
// These two lines are required to initialize Express in Cloud Code.
var express = require('express');
var parseExpressHttpsRedirect = require('parse-express-https-redirect');
var parseExpressCookieSession = require('parse-express-cookie-session');
var app = express();
var sess;
Parse.initialize("eXr5eE3ff6vTMkTqsWe373eVZbuOLtafn7mFwlI2","5mX4KetLYXXusfdk6nObvgi615o3FghX1eXq9PXW");
Parse.User.enableRevocableSession();

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
	sess.flagHasPrev;
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
	if(!(typeof query.fl === 'undefined') && query.fl!=""){
		// marking this /search comes from notifications
		sess.flagHasPrev = 1;
	} else {
		// marking this /search comes from shared link
		sess.flagHasPrev = 0;
	}
	
	var ecardInfoClass = Parse.Object.extend("ECardInfo");
	var query = new Parse.Query(ecardInfoClass);
	query.get(sess.id, {
		success: function(object) {
			console.log(sess.id);
			var targetCompany = (typeof object.get("company") === 'undefined') ? "" : object.get("company").toLowerCase().replace(/^[ ]+|[ ]+$/g,'');
			if(targetCompany != ""){
				// if the user has specified a company, display logo as well
				var ecardTemplateClass = Parse.Object.extend("ECardTemplate");
				var queryLogo = new Parse.Query(ecardTemplateClass);
				queryLogo.equalTo("companyNameLC", targetCompany);
				queryLogo.find({
					success: function(resultsLogo) {
						var logoURL = "http://www.micklestudios.com/assets/img/emptylogo.png";
						if(!(typeof resultsLogo === 'undefined') && resultsLogo.length != 0){
							console.log('found logo!');
							logoURL = resultsLogo[0].get("companyLogo").url();
						}								
						var collectedData = { 
							ecardId: sess.id,
							flagHasPrev: sess.flagHasPrev,
							firstName: (typeof object.get("firstName") === 'undefined') ? "(Undisclosed Name)" : object.get("firstName"),
							lastName: (typeof object.get("lastName") === 'undefined') ? "" : object.get("lastName"),
							company: (typeof object.get("company") === 'undefined') ? "(Undisclosed Company)" : object.get("company"),
							title: (typeof object.get("title") === 'undefined') ? "(Undisclosed Position)" : object.get("title"),
							city: (typeof object.get("city") === 'undefined') ? "(Undisclosed Location)" : object.get("city"),
							portrait_url: object.get("portrait").url(),
							companyLogo_url: logoURL,
						};					
						if(sess.flagHasPrev == 1) {
							// if this /search comes from notification, do not persist the collecting card page.
							sess.id = '';
						}
						if(Parse.User.current()) {
							// If the user has already login, don't offer login/signup
							res.render('ecardloggedin.ejs', collectedData);
						} else {
							// If the user has not login, show login/signup as well
							res.render('ecardgrow.ejs', collectedData);
						}		
					},
					error: function(error) {
						console.log('error finding logo, display default');
						var logoURL = "http://www.micklestudios.com/assets/img/emptylogo.png";														
						var collectedData = { 
							ecardId: sess.id,
							flagHasPrev: sess.flagHasPrev,
							firstName: (typeof object.get("firstName") === 'undefined') ? "(Undisclosed Name)" : object.get("firstName"),
							lastName: (typeof object.get("lastName") === 'undefined') ? "" : object.get("lastName"),
							company: (typeof object.get("company") === 'undefined') ? "(Undisclosed Company)" : object.get("company"),
							title: (typeof object.get("title") === 'undefined') ? "(Undisclosed Position)" : object.get("title"),
							city: (typeof object.get("city") === 'undefined') ? "(Undisclosed Location)" : object.get("city"),
							portrait_url: object.get("portrait").url(),
							companyLogo_url: logoURL,
						};					
						if(sess.flagHasPrev == 1) {
							// if this /search comes from notification, do not persist the collecting card page.
							sess.id = '';
						}
						if(Parse.User.current()) {
							// If the user has already login, don't offer login/signup
							res.render('ecardloggedin.ejs', collectedData);
						} else {
							// If the user has not login, show login/signup as well
							res.render('ecardgrow.ejs', collectedData);
						}
					}
				});
			} else{
				console.log('user has not specified company name');
				// if the user has not specified a log, display default
				var logoURL = "http://www.micklestudios.com/assets/img/emptylogo.png";	
				var collectedData = { 
					ecardId: sess.id,
					flagHasPrev: sess.flagHasPrev,
					firstName: (typeof object.get("firstName") === 'undefined') ? "(Undisclosed Name)" : object.get("firstName"),
					lastName: (typeof object.get("lastName") === 'undefined') ? "" : object.get("lastName"),
					company: (typeof object.get("company") === 'undefined') ? "(Undisclosed Company)" : object.get("company"),
					title: (typeof object.get("title") === 'undefined') ? "(Undisclosed Position)" : object.get("title"),
					city: (typeof object.get("city") === 'undefined') ? "(Undisclosed Location)" : object.get("city"),
					portrait_url: object.get("portrait").url(),
					companyLogo_url: logoURL,
				};					
				if(sess.flagHasPrev == 1) {
					// if this /search comes from notification, do not persist the collecting card page.
					sess.id = '';
				}
				if(Parse.User.current()) {
					// If the user has already login, don't offer login/signup
					res.render('ecardloggedin.ejs', collectedData);
				} else {
					// If the user has not login, show login/signup as well
					res.render('ecardgrow.ejs', collectedData);
				}	
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
						var whenMet = '';
						var whenMetDate = noteObj.get("whenMet");
						if(!(typeof whenMetDate === 'undefined') && whenMetDate != '') {							
							whenMet = whenMetDate.toString();
							splitDate = whenMet.split(" ");
							whenMet= splitDate[1] + " "+ splitDate[2] + " " + splitDate[3];
						}
						
						var targetCompany = (typeof infoObj.get("company") === 'undefined') ? "" : infoObj.get("company").toLowerCase().replace(/^[ ]+|[ ]+$/g,'');
						if(targetCompany != ""){
							// if the user has specified a company, display logo as well
							var ecardTemplateClass = Parse.Object.extend("ECardTemplate");
							var queryLogo = new Parse.Query(ecardTemplateClass);
							queryLogo.equalTo("companyNameLC", targetCompany);
							queryLogo.find({
								success: function(resultsLogo) {
									var logoURL;
									if(!(typeof resultsLogo === 'undefined') && resultsLogo.length != 0){
										console.log('found logo!');
										logoURL = resultsLogo[0].get("companyLogo").url();
										console.log(logoURL);
									} else {
										console.log('no logo!');
										logoURL = "http://www.micklestudios.com/assets/img/emptylogo.png";
									}				
									var collectedData = { 
										noteId: noteId,
										firstName: (typeof infoObj.get("firstName") === 'undefined') ? "(Undisclosed Name)" : infoObj.get("firstName"),
										lastName: (typeof infoObj.get("lastName") === 'undefined') ? "" : infoObj.get("lastName"),
										company: (typeof infoObj.get("company") === 'undefined') ? "(Undisclosed Company)" : infoObj.get("company"),
										title: (typeof infoObj.get("title") === 'undefined') ? "(Undisclosed Position)" : infoObj.get("title"),
										city: (typeof infoObj.get("city") === 'undefined') ? "(Undisclosed Location)" : infoObj.get("city"),
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
										whereMet: (typeof noteObj.get("event_met") === 'undefined') ? "" : noteObj.get("where_met"),
										eventMet: (typeof noteObj.get("event_met") === 'undefined') ? "" : noteObj.get("event_met"),
										notes: (typeof noteObj.get("notes") === 'undefined') ? "" : noteObj.get("notes"),
										companyLogo_url: logoURL,
									};	
									console.log('before rendering');							
									res.render('notedetails.ejs', collectedData);
								},
								error: function(error) {
									console.log('error finding logo, display default');
									var logoURL = "http://www.micklestudios.com/assets/img/emptylogo.png";														
									var collectedData = { 
										noteId: noteId,
										firstName: (typeof infoObj.get("firstName") === 'undefined') ? "(Undisclosed Name)" : infoObj.get("firstName"),
										lastName: (typeof infoObj.get("lastName") === 'undefined') ? "" : infoObj.get("lastName"),
										company: (typeof infoObj.get("company") === 'undefined') ? "(Undisclosed Company)" : infoObj.get("company"),
										title: (typeof infoObj.get("title") === 'undefined') ? "(Undisclosed Position)" : infoObj.get("title"),
										city: (typeof infoObj.get("city") === 'undefined') ? "(Undisclosed Location)" : infoObj.get("city"),
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
										whereMet: (typeof noteObj.get("event_met") === 'undefined') ? "" : noteObj.get("where_met"),
										eventMet: (typeof noteObj.get("event_met") === 'undefined') ? "" : noteObj.get("event_met"),
										notes: (typeof noteObj.get("notes") === 'undefined') ? "" : noteObj.get("notes"),
										companyLogo_url: logoURL,
									};					
									res.render('notedetails.ejs', collectedData);
								}
							});
						} else{
							console.log('user has not specified company name');
							// if the user has not specified a log, display default
							var logoURL = "http://www.micklestudios.com/assets/img/emptylogo.png";	
							var collectedData = { 
								noteId: noteId,
								firstName: (typeof infoObj.get("firstName") === 'undefined') ? "(Undisclosed Name)" : infoObj.get("firstName"),
								lastName: (typeof infoObj.get("lastName") === 'undefined') ? "" : infoObj.get("lastName"),
								company: (typeof infoObj.get("company") === 'undefined') ? "(Undisclosed Company)" : infoObj.get("company"),
								title: (typeof infoObj.get("title") === 'undefined') ? "(Undisclosed Position)" : infoObj.get("title"),
								city: (typeof infoObj.get("city") === 'undefined') ? "(Undisclosed Location)" : infoObj.get("city"),
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
								whereMet: (typeof noteObj.get("event_met") === 'undefined') ? "" : noteObj.get("where_met"),
								eventMet: (typeof noteObj.get("event_met") === 'undefined') ? "" : noteObj.get("event_met"),
								notes: (typeof noteObj.get("notes") === 'undefined') ? "" : noteObj.get("notes"),
								companyLogo_url: logoURL,
							};					
							res.render('notedetails.ejs', collectedData);
						}
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
				var whenMetString = req.body.whenMet;
				var whenMet = new Date(whenMetString);	
				object.set("whenMet", whenMet);
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
											  var whenMet = convobj.createdAt.toString();
											  splitDate = whenMet.split(" ");
											  whenMet= splitDate[1] + " "+ splitDate[2];
											  userinfoObjs[tasksToDo] = {
															cardId : rstcard.id,
															firstName: (typeof rstcard.get("firstName") === 'undefined') ? "(Undisclosed Name)" : rstcard.get("firstName"),
															lastName: (typeof rstcard.get("lastName") === 'undefined') ? "" : rstcard.get("lastName"),
															company: (typeof rstcard.get("company") === 'undefined') ? "(Undisclosed Company)" : rstcard.get("company"),
															title: (typeof rstcard.get("title") === 'undefined') ? "(Undisclosed Position)" : rstcard.get("title"),
															city: (typeof rstcard.get("city") === 'undefined') ? "(Undisclosed Location)" : rstcard.get("city"),													
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
																firstName: (typeof rstcard.get("firstName") === 'undefined') ? "(Undisclosed Name)" : rstcard.get("firstName"),
																lastName: (typeof rstcard.get("lastName") === 'undefined') ? "" : rstcard.get("lastName"),
																company: (typeof rstcard.get("company") === 'undefined') ? "(Undisclosed Company)" : rstcard.get("company"),
																title: (typeof rstcard.get("title") === 'undefined') ? "(Undisclosed Position)" : rstcard.get("title"),
																city: (typeof rstcard.get("city") === 'undefined') ? "(Undisclosed Location)" : rstcard.get("city"),												
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
				if(req.body.name != "" && typeof(req.body.name) != "undefined"){
					splitName = req.body.name.split(" ");
					firstName = "";
					lastName = "";
					if(splitName.length == 1){
						// first name only
						firstName = splitName[0];
						lastName = "";
					} else{
						// at least 2 segments
						for(var i=0; i< splitName.length-2; i++){
							firstName = firstName + splitName[i] + " ";
						} 
						for(var i=splitName.length-2; i< splitName.length-1; i++){
							firstName = firstName + splitName[i];
						}
						lastName = splitName[splitName.length-1];
					}
					if(firstName != "" && typeof(firstName) != "undefined"){
						object.set("firstName", firstName);
					} else {
						object.unset("firstName");
					}
					if(lastName != "" && typeof(lastName) != "undefined"){
						object.set("lastName", lastName);
					} else {						
						object.unset("lastName");
					}
					
				} else{
					// if the name is left empty
					object.set("firstName", "My");
					object.set("lastName", "Name?");
				}
				
				// the rest of main card
				if(req.body.title != "" && typeof(req.body.title) != "undefined"){
					object.set("title", req.body.title);
				} else{
					object.unset("title");
				}
				if(req.body.company != "" && typeof(req.body.company) != "undefined"){
					object.set("company", req.body.company);
					
					// create Ecardtemplate regardless of duplication -- gatekeeper will make sure that part				
					var templateObject = new Parse.Object('ECardTemplate');
					var usrACL = new Parse.ACL();
					usrACL.setPublicReadAccess(true);
					usrACL.setPublicWriteAccess(false);
					templateObject.setACL(usrACL);
					templateObject.set("companyName", req.body.company.replace(/^[ ]+|[ ]+$/g,''));
					templateObject.set("companyNameLC", req.body.company.toLowerCase().replace(/^[ ]+|[ ]+$/g,''));
					templateObject.save({}, {
						success: function(){
							console.log('Save ecardtemplate successful');
						},
						error: function(error){
							console.log('Save ecardtemplate failed: '+ error.message);
						}
					});	
				} else{
					object.unset("company");
				}
				
				if(req.body.city != "" && typeof(req.body.city) != "undefined"){
					object.set("city", req.body.city);
				} else{
					object.set("city", "Where am I?");
				}
				// put extra info
				if(req.body.about != "" && typeof(req.body.about) != "undefined"){
					object.set("about", req.body.about);
				} else {
					object.unset("about");
				}
				if(req.body.linkedin != "" && typeof(req.body.linkedin) != "undefined"){
					object.set("linkedin", req.body.linkedin);
				} else {
					object.unset("linkedin");
				}
				if(req.body.phone != "" && typeof(req.body.phone) != "undefined"){
					object.set("phone", req.body.phone);
				} else {
					object.unset("phone");
				}
				if(req.body.message != "" && typeof(req.body.message) != "undefined"){
					object.set("message", req.body.message);
				} else {
					object.unset("message");
				}
				if(req.body.email != "" && typeof(req.body.email) != "undefined"){
					object.set("email", req.body.email);
				} else {
					object.unset("email");
				}
				if(req.body.facebook != "" && typeof(req.body.facebook) != "undefined"){
					object.set("facebook", req.body.facebook);
				} else {
					object.unset("facebook");
				}
				if(req.body.twitter != "" && typeof(req.body.twitter) != "undefined"){
					object.set("twitter", req.body.twitter);
				} else {
					object.unset("twitter");
				}
				if(req.body.googleplus != "" && typeof(req.body.googleplus) != "undefined"){
					object.set("googleplus", req.body.googleplus);
				} else {
					object.unset("googleplus");
				}
				if(req.body.web != "" && typeof(req.body.web) != "undefined"){
					object.set("web", req.body.web);
				} else {
					object.unset("web");
				}
				if(req.body.img_url != "" && typeof(req.body.img_url) != "undefined"){
					// chrome returns "" as "", but IE returns "" as undefined!
					var Image = require("parse-image");
					Parse.Cloud.httpRequest({
						url: req.body.img_url	 
					  }).then(function(response) {		
						//console.log("got img from url");
						var image = new Image();	
						return image.setData(response.buffer);					
					  }).then(function(image) {
						// Resize the image to 64x64.
						//console.log("resize img");
						return image.scale({
						  width: 200,
						  height: 200
						});
					 
					  }).then(function(image) {
						// Make sure it's a JPEG to save disk space and bandwidth.
						//console.log("format img");
						return image.setFormat("PNG");
					 
					  }).then(function(image) {
						// Get the image data in a Buffer.
						//console.log("data img");
						return image.data();
					 
					  }).then(function(buffer) {
						// Save the image into a new file.
						//console.log("make img a file");
						var base64 = buffer.toString("base64");
						var cropped = new Parse.File("newPortrait.jpg", { base64: base64 });
						return cropped.save();
					 
					  }).then(function(cropped) {
						// Attach the image file to the original object.
						//console.log("save img into obj");
						object.set("portrait", cropped);
						
						// save changes
						object.save(null, {
							
							success: function(list) {
								// upon completion, go back to homepage
								console.log('save portrait success');
								res.json({successful : true});
							},
							error: function(error) {
							  // An error occurred while saving one of the objects.
								console.log('save portrait fail');
							  res.json({successful : false});
							},
						});		
					}, function(error) {
					  // The file either could not be read, or could not be saved to Parse.
					  console.log('save temporary file to new file has failed.');
					  res.json({successful : false});
					});
				} else {
					// if portrait not updated, directly upload
					// save changes
					console.log('portrait not updated');
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
		Parse.User.current().fetch().then(function(currentUser){
			var ecardNoteClass = Parse.Object.extend("ECardNote");
			var query = new Parse.Query(ecardNoteClass);
			query.equalTo("userId", currentUser.id);
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
						// This card has not been collected ever
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
								noteObject.set("userId", currentUser.id);
								noteObject.set("whenMet", new Date());
								noteObject.save({userId: Parse.User.current().id, ecardId: object.id, EcardUpdatedAt: object.updatedAt }, {
									success: function(){
										console.log('Save note successful');
										sess.id='';
										// sess.userinfoObjs = []; // clear the search results to reflect this change
										if( typeof(req.body.flagHasPrev) != "undefined" && req.body.flagHasPrev === '1') {
											// if coming from conversations, delete conversation
											deleteConv(0, req.body.ecardId, currentUser.get('ecardId'), res);
										} else{
											// this is not from conversations, directly return
											res.json({status : 0});
										}
									},
									error: function(error){
										console.log('Save note fails');
										sess.id='';
										if( typeof(req.body.flagHasPrev) != "undefined" && req.body.flagHasPrev === '1') {
											// if coming from conversations, delete conversation
											deleteConv(9, req.body.ecardId, currentUser.get('ecardId'), res);
										} else{
											// this is not from conversations, directly return
											res.json({status : 9});
										}
									}
								});
							},
							error: function(error) {
								// If the ecard is not found, bring user to login/signup page
								console.log('Ecard not found');
								sess.id='';
								if( typeof(req.body.flagHasPrev) != "undefined" && req.body.flagHasPrev === '1') {
									// if coming from conversations, delete conversation
									deleteConv(9, req.body.ecardId, currentUser.get('ecardId'), res);
								} else{
									// this is not from conversations, directly return
									res.json({status : 9});
								}
							}
						});	
					} else {
						// the note exists, now check if it was deleted
						var foundNoteObj = results[0];
						if(foundNoteObj.get("isDeleted") == false || foundNoteObj.get("isDeleted") == "" || (typeof foundNoteObj.get("isDeleted") === 'undefined')) {
							console.log('ecard already collected');
							sess.id='';
							if( typeof(req.body.flagHasPrev) != "undefined" && req.body.flagHasPrev === '1') {
								// if coming from conversations, delete conversation
								deleteConv(2, req.body.ecardId, currentUser.get('ecardId'), res);
							} else{
								// this is not from conversations, directly return
								res.json({status : 2});
							}
						} else {
							// if the note existed but deleted, flip the flag
							foundNoteObj.set("isDeleted", false);
							foundNoteObj.save({}, {
								success: function(){
									sess.id='';
									console.log('note revived');
									if( typeof(req.body.flagHasPrev) != "undefined" && req.body.flagHasPrev === '1') {
										// if coming from conversations, delete conversation
										deleteConv(0, req.body.ecardId, currentUser.get('ecardId'), res);
									} else{
										// this is not from conversations, directly return
										res.json({status : 0});
									}
								},
								error: function(error){
									sess.id='';
									console.log('Save note fails');
									if( typeof(req.body.flagHasPrev) != "undefined" && req.body.flagHasPrev === '1') {
										// if coming from conversations, delete conversation
										deleteConv(9, req.body.ecardId, currentUser.get('ecardId'), res);
									} else{
										// this is not from conversations, directly return
										res.json({status : 9});
									}
								}
							});
						}
					} 
				},
				error: function(error) {
					console.log('error finding my ecardinfo');
					sess.id='';
					if( typeof(req.body.flagHasPrev) != "undefined" && req.body.flagHasPrev === '1') {
						// if coming from conversations, delete conversation
						deleteConv(9, req.body.ecardId, currentUser.get('ecardId'), res);
					} else{
						// this is not from conversations, directly return
						res.json({status : 9});
					}
				}
			});	
		}, function(error){
			console.log('current session error');
			sess.id='';
			res.json({status : 9});
		});
	} else {
		// discard
		sess.id='';
		console.log('discarded ecard');
		if( typeof(req.body.flagHasPrev) != "undefined" && req.body.flagHasPrev === '1') {
			// if coming from conversations, delete conversation, but first need to acquire currentUser
			Parse.User.current().fetch().then(function(currentUser){
				sess.id='';
				deleteConv(3, req.body.ecardId, currentUser.get('ecardId'), res);
			}, function(error){
				console.log('current session error');
				sess.id='';
				res.json({status : 9});
			});
			
		} else{
			// this is not from conversations, directly return
			res.json({status : 3});
		}
	}
});

function deleteConv(statusCode, partyAEcardId, partyBEcardId, res) {
	console.log('Inside deleteConv');
	var convClass = Parse.Object.extend("Conversations");
	var query = new Parse.Query(convClass);
	query.equalTo("partyA", partyAEcardId);
	query.equalTo("partyB", partyBEcardId);			
	query.find({
		success: function(results) {
			if(typeof(results) === "undefined" || results.length === 0){
				// conversation doesn't exist, do nothing				
				res.json({status : statusCode});
			} else {
				// This conversation does exist, delete it
				for(var i=0 ; i< results.length ; i++) {
					results[i].set("isDeleted", true);											
				}
				Parse.Object.saveAll(results, {
					success: function(){
						console.log('Delete conversation successful');
						res.json({status : statusCode});
					},
					error: function(error){
						console.log('Delete conversation fails: '+ error.message);
						res.json({status : 9});
					}
				});	
			} 
		},
		error: function(error) {
			// conversation doesn't exist
			console.log('find conversation fails: '+ error.message);
			res.json({status : 9});
		}
	});	
	
}

app.post('/shareback', function(req, res){
	// create conversation pointing to the recipient	
	Parse.User.current().fetch().then(function(currentUser){
		var ecardInfoClass = Parse.Object.extend("ECardInfo");	
		var query = new Parse.Query(ecardInfoClass);
		query.get(req.body.ecardId, {
			success: function(object) {
				// upon finding the targetCard
				
				  
				  
				  var ecardInfoClass = Parse.Object.extend("ECardInfo");	
				var querySelf = new Parse.Query(ecardInfoClass);
				querySelf.get(currentUser.get('ecardId'), {
					success: function(objectSelf) {
						// send push 
						var str1 = "Hi, this is ";
						var str2 = objectSelf.get("firstName");
						var str3 = objectSelf.get("lastName");
						var str4 = ", please save my card.";
						str1 = str1.concat(str2);
						str1 = str1.concat(" ");
						str1 = str1.concat(str3);
						var message = str1.concat(str4);
					  Parse.Cloud.run('sendPushToUser', {targetEcardId: object.id, message: message}, {
						  success: function(ratings) {
							// 
						  },
						  error: function(error) {
						  }
						});
					  
					  
					},
					error: function(error){				
						
					}
				});
				  
				// create the conversation pointing to the targetCard
				var convObject = new Parse.Object('Conversations');
				var usrACL = new Parse.ACL();
				usrACL.setPublicReadAccess(false);
				usrACL.setPublicWriteAccess(false);
				usrACL.setReadAccess(currentUser.id, true);
				usrACL.setWriteAccess(currentUser.id, true);
				usrACL.setReadAccess(object.get('userId'), true);
				usrACL.setWriteAccess(object.get('userId'), true);
				convObject.setACL(usrACL);
				convObject.set("partyA", currentUser.get('ecardId'));
				convObject.set("partyB", object.id);
				convObject.set("read", false);
				convObject.save({}, {
					success: function(){
						console.log('Save conversation successful');
						res.json({status : 0});
					},
					error: function(error){				
						// When duplication is cured and this create is rejected, somehow the object is returned as "error"
						if(  typeof(error) != "undefined" ) {
							console.log(error.get("partyA"));
							if(typeof(error.get("partyA")) != "undefined"){
								// when the returned is an object, it means it's from the duplication cure. let through
								res.json({status : 0});
							} else {
								// else it's a real error
								res.json({status : 9});
							}
						} else {
							// real error
							res.json({status : 9});
						}
					}
				});	
			},
			error: function(error) {
				// If the ecard is not found, bring user to login/signup page
				console.log('shareback fails: '+ error.message);
				res.json({status : 9});
			}
		});			
	}, function(error){
		console.log('current session error');
		res.json({status : 9});
	});
				
	
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
		
		// if no ecard to collect, redirect to dashboard
		res.redirect('/');
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
		IN.User.logout();
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
	publicURL = splitStr[8];
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
		if(publicURL != ""){
			infoObject.set("linkedin", publicURL);
		} else {						
			infoObject.unset("linkedin");
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
					
					// create conversation pointing to KnoWell CSR				
					var convObject = new Parse.Object('Conversations');
					var usrACL = new Parse.ACL();
					usrACL.setPublicReadAccess(false);
					usrACL.setPublicWriteAccess(false);
					usrACL.setReadAccess(currentUser.id, true);
					usrACL.setWriteAccess(currentUser.id, true);
					// hardcoded KnoWell CSR
					usrACL.setReadAccess('1XROtTdlZK', true);
					usrACL.setWriteAccess('1XROtTdlZK', true);
					convObject.setACL(usrACL);
					convObject.set("partyA", '6jEQUw3iMd');
					convObject.set("partyB", infoObject.id);
					convObject.set("read", false);
					convObject.save({}, {
						success: function(){
							console.log('Save CSR conversation successful');
						},
						error: function(error){
							console.log('Save CSR conversation failed: '+ error.message);
						}
					});	
					
					// if no ecard to collect, redirect to dashboard
					res.redirect('/');
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
			// redirect to dashboard
			res.redirect('/');
		}, 
		function(error){
			// Login fails, redirect to homepage, where login/signup page is shown	
			res.render('failedloginpage',{code: error.code, msg: error.message, name: req.body.name});
		})
	})
})

app.post('/deletenote', function(req, res){
	// This is to respond to user's save Ecard action
	
	console.log("here");
	
	var splitIds = req.body.noteIds.split(",");
	for(var i=0; i< splitIds.length; i++){
	console.log(splitIds[i]);
	}
					
	Parse.User.current().fetch().then(function(user){
			var ecardNoteClass = Parse.Object.extend("ECardNote");
			var query = new Parse.Query(ecardNoteClass);
			query.equalTo("userId", user.id);
			query.containedIn("objectId", splitIds);
			
			query.find({
				success: function(results) {
					if(results.length === 0){
						//note doesn't exist
						res.json({successful : true});
					} else {
						// This note does exist, delete it
						for(var i=0 ; i< results.length ; i++) {
							results[i].set("isDeleted", true);											
						}
						Parse.Object.saveAll(results, {
							success: function(){
								console.log('Delete note successful');
								res.json({successful : true});
							},
							error: function(error){
								console.log('Delete note fails');
								res.json({successful : false});
							}
						  });			
						
					} 
				},
				error: function(error) {
					//note doesn't exist
					res.json({successful : false});
				}
			});	
		}, function(error){
			console.log('current session error');
			res.json({successful : false});
		});
		
	
})

app.post('/retrievepass', function(req, res){
	// This is to respond to user's save Ecard action
	console.log("retrievepass");
	var email = req.body.email;
	Parse.User.requestPasswordReset(email , {
            success: function () {
				console.log('retrieve successful');
                res.json({successful : true, msg: ''});
            },
            error: function (error) {
                console.log('retrieve fails');
                res.json({successful : false, msg: 'fail:'+ error.message});
            }
        });
	
})

app.post('/signup', function(req, res){
	if(Parse.User.current()){
		// this is necessary, otherwise signup while a session is on-going will fail
		Parse.User.logOut();
	}
	var password = "jsdj32RIfd28UFaf2";
	var flagDefaultPassword = false;
	if(req.body.password != "" && typeof(req.body.password) != "undefined"){
		// If a password is set, use it
		password = req.body.password;
		flagDefaultPassword = false;
	} else {
		// If a password is not set, use default password and send a recovery email
		flagDefaultPassword = true;
	}	
	var user = new Parse.User(); 
	user.set("username", req.body.email); // email==username
	// user.set("name", req.body.name);
	user.set("password", password);
	user.set("email", req.body.email);
	// the ACL must be set over here, instead of on current()
	var usrACL = new Parse.ACL(Parse.User.current());
	usrACL.setPublicReadAccess(false);
	usrACL.setPublicWriteAccess(false);
	user.setACL(usrACL);
	user.signUp(null).then(function(currentUser){
		// Signup successful, redirect to homepage, where dashboard will be shown
		console.log('sign up successful');
					
		// if(flagDefaultPassword) {
		// 	Parse.User.requestPasswordReset(currentUser.getEmail());
		// }
		// Upon sign up, create the user's ECard with basically no information
		var infoObject = new Parse.Object('ECardInfo');
		var usrACL = new Parse.ACL(Parse.User.current());
		usrACL.setPublicReadAccess(true);
		usrACL.setPublicWriteAccess(false);
		infoObject.setACL(usrACL);
		// set email
		infoObject.set("email", currentUser.getEmail());
		infoObject.set("userId", currentUser.id);
		// initiate default profile portrait
		ran_min = 0;
		ran_max = 8;
		var Image = require("parse-image");
		Parse.Cloud.httpRequest({
			url: "http://www.micklestudios.com/assets/img/emptyprofile"+(Math.floor(Math.random() * (ran_max - ran_min + 1)) + ran_min)+".png"	 
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
			  width: 200,
			  height: 200
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
					
					// create conversation pointing to KnoWell CSR				
					var convObject = new Parse.Object('Conversations');
					var usrACL = new Parse.ACL();
					usrACL.setPublicReadAccess(false);
					usrACL.setPublicWriteAccess(false);
					usrACL.setReadAccess(currentUser.id, true);
					usrACL.setWriteAccess(currentUser.id, true);
					// hardcoded KnoWell CSR
					usrACL.setReadAccess('1XROtTdlZK', true);
					usrACL.setWriteAccess('1XROtTdlZK', true);
					convObject.setACL(usrACL);
					convObject.set("partyA", '6jEQUw3iMd');
					convObject.set("partyB", infoObject.id);
					convObject.set("read", false);
					convObject.save({}, {
						success: function(){
							console.log('Save CSR conversation successful');
						},
						error: function(error){
							console.log('Save CSR conversation failed: '+ error.message);
						}
					});	
					
					// if no ecard to collect, redirect to infocollector
					// this redirect statement must be included here, as save() is non-blocking
					// if not placed here, redirect will happen before save is complete
					var prefilledData = { 
							email: infoObject.get("email"),
							portrait_url: infoObject.get("portrait").url(),
							};
					res.render('infocollector.ejs', prefilledData);	
					
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
	//IN.User.logout();
	res.redirect('dummypage');
})

app.get('/', function(req, res){
	if(Parse.User.current()){
		// if the user has logged in, show dashboard
		if(!(typeof sess.id === 'undefined') && sess.id != '') {
			console.log('An Ecard to be collected');
			// If there is an ecard to be collected from the url, display the page and offer to save
			var ecardInfoClass = Parse.Object.extend("ECardInfo");
			var query = new Parse.Query(ecardInfoClass);
			query.get(sess.id, {
				success: function(object) {
					console.log(sess.id);
					var targetCompany = (typeof object.get("company") === 'undefined') ? "" : object.get("company").toLowerCase().replace(/^[ ]+|[ ]+$/g,'');
					if(targetCompany != ""){
						// if the user has specified a company, display logo as well
						var ecardTemplateClass = Parse.Object.extend("ECardTemplate");
						var queryLogo = new Parse.Query(ecardTemplateClass);
						queryLogo.equalTo("companyNameLC", targetCompany);
						queryLogo.find({
							success: function(resultsLogo) {
								var logoURL = "http://www.micklestudios.com/assets/img/emptylogo.png";
								if(!(typeof resultsLogo === 'undefined') && resultsLogo.length != 0){
									console.log('found logo!');
									logoURL = resultsLogo[0].get("companyLogo").url();
								}								
								var collectedData = { 
									ecardId: sess.id,
									flagHasPrev: 0,
									firstName: (typeof object.get("firstName") === 'undefined') ? "(Undisclosed Name)" : object.get("firstName"),
									lastName: (typeof object.get("lastName") === 'undefined') ? "" : object.get("lastName"),
									company: (typeof object.get("company") === 'undefined') ? "(Undisclosed Company)" : object.get("company"),
									title: (typeof object.get("title") === 'undefined') ? "(Undisclosed Position)" : object.get("title"),
									city: (typeof object.get("city") === 'undefined') ? "(Undisclosed Location)" : object.get("city"),
									portrait_url: object.get("portrait").url(),
									companyLogo_url: logoURL,
								};					
								// The user has already login, don't offer login/signup
								res.render('ecardloggedin.ejs', collectedData);			
							},
							error: function(error) {
								console.log('error finding logo, display default');
								var logoURL = "http://www.micklestudios.com/assets/img/emptylogo.png";														
								var collectedData = { 
									ecardId: sess.id,
									flagHasPrev: 0,
									firstName: (typeof object.get("firstName") === 'undefined') ? "(Undisclosed Name)" : object.get("firstName"),
									lastName: (typeof object.get("lastName") === 'undefined') ? "" : object.get("lastName"),
									company: (typeof object.get("company") === 'undefined') ? "(Undisclosed Company)" : object.get("company"),
									title: (typeof object.get("title") === 'undefined') ? "(Undisclosed Position)" : object.get("title"),
									city: (typeof object.get("city") === 'undefined') ? "(Undisclosed Location)" : object.get("city"),
									portrait_url: object.get("portrait").url(),
									companyLogo_url: logoURL,
								};					
								// The user has already login, don't offer login/signup
								res.render('ecardloggedin.ejs', collectedData);
							}
						});
					} else{
						console.log('user has not specified company name');
						// if the user has not specified a log, display default
						var logoURL = "http://www.micklestudios.com/assets/img/emptylogo.png";	
						var collectedData = { 
							ecardId: sess.id,
							flagHasPrev: 0,
							firstName: (typeof object.get("firstName") === 'undefined') ? "(Undisclosed Name)" : object.get("firstName"),
							lastName: (typeof object.get("lastName") === 'undefined') ? "" : object.get("lastName"),
							company: (typeof object.get("company") === 'undefined') ? "(Undisclosed Company)" : object.get("company"),
							title: (typeof object.get("title") === 'undefined') ? "(Undisclosed Position)" : object.get("title"),
							city: (typeof object.get("city") === 'undefined') ? "(Undisclosed Location)" : object.get("city"),
							portrait_url: object.get("portrait").url(),
							companyLogo_url: logoURL,
						};					
						// The user has already login, don't offer login/signup
						res.render('ecardloggedin.ejs', collectedData);			
					}
				},
				error: function(error) {
					// If the ecard is not found, bring user to login/signup page
					sess.id = '';
					res.redirect('/');
				}
			});						
		} else {
			// no card to be collected, go to dashboard
			// Must use fetch(), previously tried current().get("name"), returns null
			Parse.User.current().fetch().then(function(user){
				var ecardInfoClass = Parse.Object.extend("ECardInfo");
				var query = new Parse.Query(ecardInfoClass);
				query.get(user.get("ecardId"), {
					success: function(object) {
						// display the current user's ecard
						var targetCompany = (typeof object.get("company") === 'undefined') ? "" : object.get("company").toLowerCase().replace(/^[ ]+|[ ]+$/g,'');
						if(targetCompany != ""){
							// if the user has specified a company, display logo as well
							var ecardTemplateClass = Parse.Object.extend("ECardTemplate");
							var queryLogo = new Parse.Query(ecardTemplateClass);
							queryLogo.equalTo("companyNameLC", targetCompany);
							queryLogo.find({
								success: function(resultsLogo) {
									var logoURL = "http://www.micklestudios.com/assets/img/emptylogo.png";
									if(!(typeof resultsLogo === 'undefined') && resultsLogo.length != 0){
										console.log('found logo!');
										logoURL = resultsLogo[0].get("companyLogo").url();
									}								
									var collectedData = { 
										ecardId: object.id,
										firstName: (typeof object.get("firstName") === 'undefined') ? "(Undisclosed Name)" : object.get("firstName"),
										lastName: (typeof object.get("lastName") === 'undefined') ? "" : object.get("lastName"),
										company: (typeof object.get("company") === 'undefined') ? "(Undisclosed Company)" : object.get("company"),
										title: (typeof object.get("title") === 'undefined') ? "(Undisclosed Position)" : object.get("title"),
										city: (typeof object.get("city") === 'undefined') ? "(Undisclosed Location)" : object.get("city"),
										username: user.get('username'),
										portrait_url: object.get("portrait").url(),
										companyLogo_url: logoURL,
									};					
									// The user has already login, don't offer login/signup
									res.render('dashboard.ejs', collectedData);					
								},
								error: function(error) {
									console.log('error finding logo, display default');
									var logoURL = "http://www.micklestudios.com/assets/img/emptylogo.png";	
									var collectedData = { 
										ecardId: object.id,
										firstName: (typeof object.get("firstName") === 'undefined') ? "(Undisclosed Name)" : object.get("firstName"),
										lastName: (typeof object.get("lastName") === 'undefined') ? "" : object.get("lastName"),
										company: (typeof object.get("company") === 'undefined') ? "(Undisclosed Company)" : object.get("company"),
										title: (typeof object.get("title") === 'undefined') ? "(Undisclosed Position)" : object.get("title"),
										city: (typeof object.get("city") === 'undefined') ? "(Undisclosed Location)" : object.get("city"),
										username: user.get('username'),
										portrait_url: object.get("portrait").url(),
										companyLogo_url: logoURL,
									};					
									// The user has already login, don't offer login/signup
									res.render('dashboard.ejs', collectedData);			
								}
							});
						} else{
							console.log('user has not specified company name');
							// if the user has not specified a log, display default
							var logoURL = "http://www.micklestudios.com/assets/img/emptylogo.png";	
							var collectedData = { 
								ecardId: object.id,
								firstName: (typeof object.get("firstName") === 'undefined') ? "(Undisclosed Name)" : object.get("firstName"),
								lastName: (typeof object.get("lastName") === 'undefined') ? "" : object.get("lastName"),
								company: (typeof object.get("company") === 'undefined') ? "(Undisclosed Company)" : object.get("company"),
								title: (typeof object.get("title") === 'undefined') ? "(Undisclosed Position)" : object.get("title"),
								city: (typeof object.get("city") === 'undefined') ? "(Undisclosed Location)" : object.get("city"),
								username: user.get('username'),
								portrait_url: object.get("portrait").url(),
								companyLogo_url: logoURL,
							};					
							// The user has already login, don't offer login/signup
							res.render('dashboard.ejs', collectedData);			
						}							 
					},
					error: function(error) {
						console.log('error finding my ecardinfo');
						Parse.User.logOut();
						res.redirect('/');
					}
				});	
			}, function(error){
				if(Parse.User.current()){
					// this is necessary, otherwise signup while a session is on-going will fail
					Parse.User.logOut();
					res.redirect('/');
				}
			});
		}
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
