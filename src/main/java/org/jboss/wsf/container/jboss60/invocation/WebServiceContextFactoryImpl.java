/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.wsf.container.jboss60.invocation;

import org.jboss.wsf.spi.invocation.*;

import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;

/**
 * @author Heiko.Braun@jboss.com
 *         Created: Jul 25, 2007
 */
public class WebServiceContextFactoryImpl extends WebServiceContextFactory
{
   public ExtensibleWebServiceContext newWebServiceContext(InvocationType type, MessageContext messageContext)
   {
      ExtensibleWebServiceContext context = null;

      //checking for a provided WebServiceContext in the MessageContext; to be removed after EJBTHREE-1604
      WebServiceContext providedContext = (WebServiceContext)messageContext.get(WebServiceContext.class.toString());
      if (providedContext != null)
         context = new WebServiceContextDelegate(providedContext);
      else if (type.toString().indexOf("EJB") != -1 || type.toString().indexOf("MDB") != -1)
         context = new WebServiceContextEJB(messageContext);
      else
         context = new WebServiceContextJSE(messageContext);

      return context;
   }
}