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
package org.jboss.wsf.container.jboss50.ejb3;

import org.jboss.wsf.spi.deployment.integration.WebServiceDeployment;
import org.jboss.wsf.spi.deployment.integration.WebServiceDeclaration;
import org.jboss.ejb3.Ejb3Deployment;
import org.jboss.ejb3.Container;
import org.jboss.ejb3.EJBContainer;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Adopts the new WS-EJB3 API to legacy EJB3 codebase while the
 * EJB3 hasn't updated to the new integration interfaces.
 * 
 * <p/>
 * TODO: This should be implemented by {@link org.jboss.ejb3.Ejb3Deployment}
 *
 * @author Heiko.Braun <heiko.braun@jboss.com>
 */
public class WebServiceDeploymentApiAdapter implements WebServiceDeployment
{
   private Ejb3Deployment ejb3Deployment;

   public static WebServiceDeploymentApiAdapter createInstance(Ejb3Deployment ejb3Deployment)
   {
      if(null==ejb3Deployment)
         throw new IllegalArgumentException("Ejb3Deployment.class cannot be null");

      return new WebServiceDeploymentApiAdapter(ejb3Deployment);
   }

   private WebServiceDeploymentApiAdapter(Ejb3Deployment ejb3Deployment)
   {
      this.ejb3Deployment = ejb3Deployment;
   }

   public List<WebServiceDeclaration> getServiceEndpoints()
   {
      List<WebServiceDeclaration> container = new ArrayList<WebServiceDeclaration>();

      Iterator<Container> it = ejb3Deployment.getEjbContainers().values().iterator();
      while(it.hasNext())
      {
         final EJBContainer c = (EJBContainer)it.next();
         container.add(
           new WebServiceDeclaration()
           {

              public <T extends java.lang.annotation.Annotation> T getAnnotation(Class<T> t)
              {
                 return c.getAnnotation(t);
              }


              public String getComponentName()
              {
                 return c.getEjbName();
              }

              public String getComponentClassName()
              {
                 return c.getBeanClassName();  
              }
           }

         );
      }
      return container;

   }
}