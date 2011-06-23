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
package org.jboss.webservices.integration.security;

import java.util.ResourceBundle;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.logging.Logger;
import org.jboss.security.plugins.JaasSecurityDomain;
import org.jboss.ws.api.util.BundleUtils;
import org.jboss.wsf.spi.security.JAASSecurityDomainAdaptor;
import org.jboss.wsf.spi.security.JAASSecurityDomainAdaptorResolver;

/**
 * Lookup JAASSecurityAdaptor from given JNDI location
 *
 * @author alessio.soldano@jboss.com
 * @since 14-Dec-2010
 */
public class JaasSecurityDomainAdaptorResolverImpl implements JAASSecurityDomainAdaptorResolver
{
   private static final ResourceBundle bundle = BundleUtils.getBundle(JaasSecurityDomainAdaptorResolverImpl.class);
   public JAASSecurityDomainAdaptor lookup(String jndi) throws Exception
   {
      InitialContext ic = null;
      try
      {
         ic = new InitialContext();
         Object o = ic.lookup(jndi);
         if (!(o instanceof JaasSecurityDomain))
         {
            throw new Exception(BundleUtils.getMessage(bundle, "NOT_BUNDTO_JAASSECURITYDOMAIN", new Object[] {jndi,o.getClass().getName()}));
         }
         return new JaasSecurityDomainAdaptorImpl((JaasSecurityDomain)o);
      }
      catch (NamingException e)
      {
         throw new Exception(BundleUtils.getMessage(bundle, "JNDI_FAILURE_HANDLING",  jndi),  e);
      }
      finally
      {
         if (ic != null)
         {
            try
            {
               ic.close();
            }
            catch (NamingException e)
            {
               Logger.getLogger(JaasSecurityDomainAdaptorImpl.class).warn(BundleUtils.getMessage(bundle, "FAILED_TO_CLOSE_INITIALCONTEXT", this),  e);
            }
         }
      }
   }
}
