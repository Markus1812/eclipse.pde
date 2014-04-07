
/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package m;

public class MethodReference {
	/**
	 * @noreference This method is not intended to be referenced by clients.
	
	 * @return
	 */
	public static void method1() {
		return ;
	}
	/**
	 * @noreference This method is not intended to be referenced by clients.
	 * @return
	 */
	public  void method2() {
		return ;
	}


}
