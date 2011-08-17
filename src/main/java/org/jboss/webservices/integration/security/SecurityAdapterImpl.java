/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
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

import java.security.AccessController;
import java.security.Principal;
import java.security.PrivilegedAction;

import javax.security.auth.Subject;

import org.jboss.security.SecurityAssociation;
import org.jboss.security.SecurityContext;
import org.jboss.security.SecurityContextAssociation;
import org.jboss.wsf.spi.invocation.SecurityAdaptor;

/**
 * The JBoss AS specific SecurityAssociation adapter.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @author <a href="mailto:tdiesler@redhat.com">Thomas Diesler</a>
 */
final class SecurityAdapterImpl implements SecurityAdaptor
{
   /**
    * Constructor.
    */
   SecurityAdapterImpl()
   {
      super();
   }

   /**
    * @see org.jboss.wsf.spi.invocation.SecurityAdaptor#getPrincipal()
    *
    * @return principal
    */
   public Principal getPrincipal()
   {
      return SecurityAssociation.getPrincipal();
   }

   /**
    * @see org.jboss.wsf.spi.invocation.SecurityAdaptor#setPrincipal(Principal)
    *
    * @param principal principal
    */
   public void setPrincipal(final Principal principal)
   {
      SecurityAssociation.setPrincipal(principal);
   }

   /**
    * @see org.jboss.wsf.spi.invocation.SecurityAdaptor#getCredential()
    *
    * @return credential
    */
   public Object getCredential()
   {
      return SecurityAssociation.getCredential();
   }

   /**
    * @see org.jboss.wsf.spi.invocation.SecurityAdaptor#setCredential(Object)
    *
    * @param credential credential
    */
   public void setCredential(final Object credential)
   {
      SecurityAssociation.setCredential(credential);
   }

   /**
    * @see org.jboss.wsf.spi.invocation.SecurityAdaptor#pushSubjectContext(Subject, Principal, Object)
    *
    * @param subject subject
    * @param principal principal
    * @param credential credential
    */
   public void pushSubjectContext(final Subject subject, final Principal principal, final Object credential)
   {
      AccessController.doPrivileged(new PrivilegedAction<Void>()
      {

         public Void run()
         {
            final SecurityContext securityContext = SecurityContextAssociation.getSecurityContext();
            if (securityContext == null)
            {
               throw new IllegalStateException("Security Context is null");
            }

            securityContext.getUtil().createSubjectInfo(principal, credential, subject);

            return null;
         }
      });
   }
}
