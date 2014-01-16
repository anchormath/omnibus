App.Router.map(function() {
    this.resource('index', { path:'/'}); 
    this.resource('topics');
    this.resource('topic', { path:'/topic/:topic_id' });
    this.resource('system');
});

App.FridgeRoute = Ember.Route.extend({
	model: function(params) {
		return App.Dao.topic(params.topic_id);
  	}
});

App.FridgesRoute = Ember.Route.extend({
	model: function() {
		return App.Dao.topics();
  	}
});