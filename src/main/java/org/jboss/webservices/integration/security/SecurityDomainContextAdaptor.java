/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
import java.util.Set;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.security.auth.Subject;

import org.jboss.security.AuthenticationManager;
import org.jboss.security.RealmMapping;
import org.jboss.security.SecurityContext;
import org.jboss.security.SecurityContextAssociation;

/**
 * org.jboss.wsf.spi.security.SecurityDomainContext implementation relying on AuthenticationManager
 *
 * @author alessio.soldano@jboss.com
 * @since 18-May-2011
 */
public final class SecurityDomainContextAdaptor implements org.jboss.wsf.spi.security.SecurityDomainContext {

    private AuthenticationManager authenticationManager;
    private RealmMapping realmMapping;
    
    
    public SecurityDomainContextAdaptor() {
       //NOOP
    }
    
    private void setupAuthenticationManager() {
       if (authenticationManager == null) {
          try
          {
             Context ctx = new InitialContext();
             Object obj = ctx.lookup("java:comp/env/security/securityMgr");
             authenticationManager = (AuthenticationManager)obj;
             realmMapping = (RealmMapping)authenticationManager;
          }
          catch (NamingException ne)
          {
             throw new RuntimeException("Unable to lookup AuthenticationManager", ne);
          }
       }
    }

   @Override
    public boolean isValid(Principal principal, Object credential, Subject activeSubject) {
        setupAuthenticationManager();
        return authenticationManager.isValid(principal, credential, activeSubject);
    }

    @Override
    public boolean doesUserHaveRole(Principal principal, Set<Principal> roles) {
        setupAuthenticationManager();
        return realmMapping.doesUserHaveRole(principal, roles);
    }

    @Override
    public String getSecurityDomain() {
        setupAuthenticationManager();
        return authenticationManager.getSecurityDomain();
    }

    @Override
    public Set<Principal> getUserRoles(Principal principal) {
        setupAuthenticationManager();
        return realmMapping.getUserRoles(principal);
    }

    @Override
    public void pushSubjectContext(final Subject subject, final Principal principal, final Object credential) {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            public Void run() {
                SecurityContext securityContext = SecurityContextAssociation.getSecurityContext();
                if (securityContext == null) {
                   throw new IllegalStateException("Security Context is null");
                }
                securityContext.getUtil().createSubjectInfo(principal, credential, subject);
                return null;
            }
        });
    }
}
