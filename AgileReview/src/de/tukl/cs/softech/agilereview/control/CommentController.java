package de.tukl.cs.softech.agilereview.control;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Observable;
import java.util.regex.Pattern;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;

import agileReview.softech.tukl.de.CommentDocument.Comment;

import de.tukl.cs.softech.agilereview.tools.PropertiesManager;
import de.tukl.cs.softech.agilereview.view.CommentTableView;
import de.tukl.cs.softech.agilereview.view.DetailView;
import de.tukl.cs.softech.agilereview.view.ReviewExplorer;
import de.tukl.cs.softech.agilereview.view.detail.CommentDetail;
import de.tukl.cs.softech.agilereview.view.detail.ReviewDetail;

/**
 * The CommentController describes an interface for some complex interactions of the different ViewParts.
 * For example the CommentController observes all selections of the CommentTableView and the current active
 * ReviewID of the ReviewExplorer.<br>
 * Besides this the CommentController provides Listener for any save action according the persistent storage.
 */
public class CommentController extends Observable implements Listener, ISelectionChangedListener, IPartListener {
	
	/**
	 * unique instance of CommentController
	 */
	private static final CommentController instance = new CommentController();
	/**
	 * unique instance of ReviewAccess
	 */
	private ReviewAccess ra = ReviewAccess.getInstance();
	/**
	 * current Selection of CommentTable
	 */
	private ArrayList<Comment> currentSelection;
	/**
	 * registered ViewParts
	 */
	private HashSet<Class<?>> registered = new HashSet<Class<?>>();
	
	/**
	 * returns the unique instance of CommentController
	 * @return the unique instance of CommentController
	 */
	public static CommentController getInstance() {
		return instance;
	}
	
	/**
	 * Creates a new instance of the CommentController
	 */
	private CommentController() {
		while(PlatformUI.getWorkbench().getActiveWorkbenchWindow() == null) {
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().addPartListener(this);
		}
	}
	
	/**
	 * manages:<br>
	 * "save" events of {@link CommentDetail} or {@link ReviewDetail}<br>
	 * "add" events of {@link CommentTableView} or {@link CommentDetail}<br>
	 * "delete" events of {@link CommentTableView} or {@link CommentDetail}
	 * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
	 */
	@Override
	public void handleEvent(Event event) {
		if((event.widget.getData()) instanceof String) {
			if(((String)event.widget.getData()).equals("save")) {
				if(registered.contains(CommentTableView.getInstance().getClass())) {
					CommentTableView.getInstance().refreshComments();
				}
			} else if(((String)event.widget.getData()).equals("delete")) {
				Iterator<Comment> it = currentSelection.iterator();
				CommentTableView ctv = CommentTableView.getInstance();
				while (it.hasNext()) {
					Comment c = it.next();
					ctv.deleteComment(c);
					try {
						ra.deleteComment(c);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				DetailView.getInstance().changeParent(DetailView.EMPTY);
				// Refresh the Review Explorer
				ReviewExplorer.getInstance().refresh();
			} else if(((String)event.widget.getData()).equals("add")) {
				if (!PropertiesManager.getInstance().getActiveReview().isEmpty())
				{
					try {
						String pathToFile = "";
						IEditorPart part = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
						if ( part instanceof ITextEditor ) {
							IEditorInput input = part.getEditorInput();
							if (input != null && input instanceof FileEditorInput) {
								pathToFile = ((FileEditorInput)input).getFile().getFullPath().toOSString().replaceFirst(Pattern.quote(System.getProperty("file.separator")), "");
							}
						}
						String activeReview = PropertiesManager.getInstance().getActiveReview();
						CommentTableView.getInstance().addComment(ra.createNewComment(activeReview , System.getProperty("user.name"), pathToFile));
						// Refresh the Review Explorer
						ReviewExplorer.getInstance().refresh();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				else
				{
					MessageDialog.openWarning(null, "Warning: No active review", "Please activate a review before adding comments!");
				}
			}
			
			// Save changes
			try {
				ra.save();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
	}

	/**
	 * hears for the current selection of the CommentTableView
	 * @see org.eclipse.jface.viewers.ISelectionChangedListener#selectionChanged(org.eclipse.jface.viewers.SelectionChangedEvent)
	 */
	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		currentSelection = new ArrayList<Comment>();
		ISelection selection = event.getSelection();		
		if (selection != null && selection instanceof IStructuredSelection) {
			IStructuredSelection sel = (IStructuredSelection) selection;			
			for (Iterator<?> iterator = sel.iterator(); iterator.hasNext();) {
				Comment selComment = (Comment)iterator.next();
				currentSelection.add(selComment);
			}
		}
	}

	/**
	 * not used
	 * @see org.eclipse.ui.IPartListener#partActivated(org.eclipse.ui.IWorkbenchPart)
	 */
	@Override
	public void partActivated(IWorkbenchPart part) {
		
	}
	
	/**
	 * not used
	 * @see org.eclipse.ui.IPartListener#partBroughtToTop(org.eclipse.ui.IWorkbenchPart)
	 */
	@Override
	public void partBroughtToTop(IWorkbenchPart part) {
		
	}
	
	/**
	 * every ViewPart of this plugin will be deregistered by the CommentController
	 * @see org.eclipse.ui.IPartListener#partClosed(org.eclipse.ui.IWorkbenchPart)
	 */
	@Override
	public void partClosed(IWorkbenchPart part) {
		System.out.println("partClosed -> "+part.getClass());
		registered.remove(part.getClass());
	}
	
	/**
	 * not used
	 * @see org.eclipse.ui.IPartListener#partDeactivated(org.eclipse.ui.IWorkbenchPart)
	 */
	@Override
	public void partDeactivated(IWorkbenchPart part) {
		
	}
	
	/**
	 * every ViewPart of this plugin will be registered by the CommentController
	 * @see org.eclipse.ui.IPartListener#partOpened(org.eclipse.ui.IWorkbenchPart)
	 */
	@Override
	public void partOpened(IWorkbenchPart part) {
		System.out.println("partOpened -> "+part.getClass());
		registered.add(part.getClass());
	}
}
