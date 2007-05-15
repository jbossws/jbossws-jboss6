/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.ws.integration.jboss50;

//$Id$

import java.util.Iterator;

import javax.annotation.security.RolesAllowed;

import org.jboss.ejb3.EJBContainer;
import org.jboss.ejb3.Ejb3Deployment;
import org.jboss.logging.Logger;
import org.jboss.ws.integration.deployment.SecurityRolesHandler;
import org.jboss.ws.integration.deployment.UnifiedDeploymentInfo;
import org.jboss.ws.utils.DOMUtils;
import org.w3c.dom.Element;

/**
 * Generate a service endpoint deployment for EJB endpoints 
 * 
 * @author Thomas.Diesler@jboss.org
 * @since 12-May-2006
 */
public class ServiceEndpointGeneratorEJB3 implements SecurityRolesHandler
{
   // logging support
   protected Logger log = Logger.getLogger(ServiceEndpointGeneratorEJB3.class);

   /** Add the roles from ejb-jar.xml to the security roles
    */
   public void addSecurityRoles(Element webApp, UnifiedDeploymentInfo udi)
   {
      Ejb3Deployment ejb3Deployment = udi.getAttachment(Ejb3Deployment.class);
      if (ejb3Deployment != null)
      {
         Iterator it = ejb3Deployment.getEjbContainers().values().iterator();
         while (it.hasNext())
         {
            EJBContainer container = (EJBContainer)it.next();
            RolesAllowed anRolesAllowed = (RolesAllowed)container.resolveAnnotation(RolesAllowed.class);
            if (anRolesAllowed != null)
            {
               for (String role : anRolesAllowed.value())
               {
                  Element securityRole = (Element)webApp.appendChild(DOMUtils.createElement("security-role"));
                  Element roleName = (Element)securityRole.appendChild(DOMUtils.createElement("role-name"));
                  roleName.appendChild(DOMUtils.createTextNode(role));
               }
            }
         }
      }
   }
}
