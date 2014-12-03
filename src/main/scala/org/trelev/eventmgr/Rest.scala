package org.trelev.eventmgr

import org.trelev.eventmgr.web.Web
import org.trelev.eventmgr.web.Api


object Rest extends App with BootedCore with CoreActors with Api with Web