/**
 *   Copyright 2005 Open Cloud Ltd.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package org.mobicents.eclipslee.servicecreation.popup.actions;

import java.util.HashMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.mobicents.eclipslee.servicecreation.wizards.service.ServiceRootSbbDialog;
import org.mobicents.eclipslee.util.SLEE;
import org.mobicents.eclipslee.util.slee.xml.components.ComponentNotFoundException;
import org.mobicents.eclipslee.util.slee.xml.components.SbbXML;
import org.mobicents.eclipslee.util.slee.xml.components.Service;
import org.mobicents.eclipslee.xml.SbbJarXML;
import org.mobicents.eclipslee.xml.ServiceXML;


/**
 * @author cath
 */
public class EditServiceRootSbbAction implements IObjectActionDelegate {
	
	/**
	 * Constructor for Action1.
	 */
	public EditServiceRootSbbAction() {
		super();
	}
	
	public EditServiceRootSbbAction(String serviceID) {
		super();
		this.serviceID = serviceID;
	}
	
	/**
	 * @see IObjectActionDelegate#setActivePart(IAction, IWorkbenchPart)
	 */
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
	}
	
	/**
	 * @see IActionDelegate#run(IAction)
	 */
	public void run(IAction action) {
		
		initialize();
		
		if (dialog == null) {
			MessageDialog.openError(new Shell(), "Error Modifying Service", getLastError());
			return;
		}
		
		// Open the dialog that was configured in initialize()
		if (dialog.open() == Window.OK) {
			
			// Update the service xml
			try {
				
				HashMap rootSbb = dialog.getRootSbb();
				int priority = dialog.getDefaultPriority();
				boolean createAddressProfileTable = dialog.getCreateAddressProfileTable();
				
				String name = (String) rootSbb.get("Name");
				String vendor = (String) rootSbb.get("Vendor");
				String version = (String) rootSbb.get("Version");
				SbbJarXML xml = (SbbJarXML) rootSbb.get("XML");
				SbbXML sbb = xml.getSbb(name, vendor, version);
				
				service.setDefaultPriority(priority);
				service.setRootSbb(sbb);
				service.setAddressProfileTable(createAddressProfileTable
							? sbb.getAddressProfileSpecAliasRef().getAlias()
							: null);				
				
				// Save the service XML
				file.setContents(serviceXML.getInputStreamFromXML(), true, true, null);
			} catch (Exception e) {
				MessageDialog.openError(new Shell(), "Error Updating Service XML",
						"Exception caught while saving Service XML: " + e.getClass().toString() + ": " + e.getMessage());
				e.printStackTrace();
				return;
			}		
		}
	}
	
	/**
	 * Get the ServiceXML data object for the current selection.
	 *
	 */
	
	private void initialize() {
		
		dialog = null;
		service = null;
		serviceXML = null;
		
		if (selection == null && selection.isEmpty()) {
			setLastError("Please select an Service's Java or XML file first.");
			return;
		}
		
		if (!(selection instanceof IStructuredSelection)) {
			setLastError("Please select an Service's Java or XML file first.");
			return;			
		}
		
		IStructuredSelection ssel = (IStructuredSelection) selection;
		if (ssel.size() > 1) {
			setLastError("This plugin only supports editing of one service at a time.");
			return;
		}
		
		// Get the first (and only) item in the selection.
		Object obj = ssel.getFirstElement();
		
		if (obj instanceof IFile) {
			file = (IFile) obj;
			
			String name = SLEE.getName(serviceID);
			String vendor = SLEE.getVendor(serviceID);
			String version = SLEE.getVersion(serviceID);
			
			try {
				serviceXML = new ServiceXML(file);
			} catch (Exception e) {
				// Handled by the next check
			}
			
			if (serviceXML == null) {
				setLastError("Unable to find the corresponding service-jar.xml for this service.");
				return;
			}
			
			try {
				service = serviceXML.getService(name, vendor, version);
			} catch (ComponentNotFoundException e) {
				setLastError("This service is not defined in this Service XML file.");
				return;
			}
		} else {
			setLastError("Unsupported object type: " + obj.getClass().toString());
			return;
		}
		
		// Open a dialog allowing the user to edit the Service's identity.
		dialog = new ServiceRootSbbDialog(new Shell(), service, file.getProject().getName());
		
		return;
	}
	
	/**
	 * @see IActionDelegate#selectionChanged(IAction, ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		this.selection = selection;	
	}
	
	private void setLastError(String error) {
		if (error == null) {
			lastError = "Success";
		} else {
			lastError = error;
		}
	}
	
	private String getLastError() {
		String error = lastError;
		setLastError(null);
		return error;
	}
	
	private ServiceRootSbbDialog dialog;
	private ISelection selection;
	private ServiceXML serviceXML;
	private Service service;
	private IFile file;
	private String lastError;
	private String serviceID;
}
