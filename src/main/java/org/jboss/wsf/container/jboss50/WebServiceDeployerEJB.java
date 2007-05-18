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
package org.jboss.wsf.container.jboss50;

//$Id$

/**
 * This web service deployer for EJB
 * 
 * @author Thomas.Diesler@jboss.org
 * @since 24-Apr-2007
 */
public class WebServiceDeployerEJB extends AbstractWebServiceDeployer
{
   private int relOrderEJB2x;
   private int relOrderEJB3;
   
   public void setRelOrderEJB2x(int relOrderEJB2x)
   {
      this.relOrderEJB2x = relOrderEJB2x;
   }
   
   public void setRelOrderEJB3(int relOrderEJB3)
   {
      this.relOrderEJB3 = relOrderEJB3;
   }
   
   @Override
   public int getRelativeOrder()
   {
      return Math.max(relOrderEJB2x, relOrderEJB3) + 1;
   }
   
}
