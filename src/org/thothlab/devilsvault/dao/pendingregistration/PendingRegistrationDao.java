package org.thothlab.devilsvault.dao.pendingregistration;

import java.util.List;

import org.thothlab.devilsvault.db.model.PendingRegistration;

public interface PendingRegistrationDao {
	public Boolean save(PendingRegistration user, String type);
	public PendingRegistration getById(int id, String table);
	public boolean update(PendingRegistration user, String type);
	public boolean deleteById(int id);
	public List<PendingRegistration> getAllPending();
}
