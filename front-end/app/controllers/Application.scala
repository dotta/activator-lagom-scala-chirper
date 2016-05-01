/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package controllers

import play.api.mvc._

class Application extends Controller {

  def index = Action {
    Ok(views.html.index.render())
  }

  def userStream(userId: String) = Action {
    Ok(views.html.index.render())
  }

  def circuitBreaker = Action {
    Ok(views.html.circuitbreaker.render())
  }

}
