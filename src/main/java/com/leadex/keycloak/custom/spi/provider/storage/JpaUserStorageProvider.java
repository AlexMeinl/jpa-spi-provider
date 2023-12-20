package com.leadex.keycloak.custom.spi.provider.storage;

import com.leadex.keycloak.custom.spi.provider.UserAdapter;
import com.leadex.keycloak.custom.spi.provider.model.CustomUser;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.apache.commons.lang3.StringUtils;
import org.keycloak.component.ComponentModel;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputUpdater;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.models.*;
import org.keycloak.models.cache.CachedUserModel;
import org.keycloak.models.cache.OnUserCache;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.user.UserLookupProvider;
import org.keycloak.storage.user.UserQueryProvider;
import org.keycloak.storage.user.UserRegistrationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Stream;

public class JpaUserStorageProvider
    implements UserStorageProvider, UserLookupProvider,
    UserRegistrationProvider, UserQueryProvider,
    CredentialInputUpdater, CredentialInputValidator, OnUserCache {

  private static final Logger log = LoggerFactory.getLogger(JpaUserStorageProvider.class);

  public static final String PASSWORD_CACHE_KEY = UserAdapter.class.getName() + ".password";

  private final EntityManager em;

  private final ComponentModel model;
  private final KeycloakSession session;

  public JpaUserStorageProvider(KeycloakSession session, ComponentModel model) {
    this.session = session;
    this.model = model;
    em = session.getProvider(JpaConnectionProvider.class, "user-store").getEntityManager();
  }

  @Override
  public void preRemove(RealmModel realm) {

  }

  @Override
  public void preRemove(RealmModel realm, GroupModel group) {

  }

  @Override
  public void preRemove(RealmModel realm, RoleModel role) {

  }

  @Override
  public void close() {
  }

  @Override
  public UserModel getUserById(RealmModel realm, String id) {
    log.info("getUserById: " + id);
    String persistenceId = StorageId.externalId(id);
    CustomUser entity = em.find(CustomUser.class, persistenceId);
    if (entity == null) {
      log.info("could not find user by id: " + id);
      return null;
    }
    return new UserAdapter(session, realm, model, entity);
  }

  @Override
  public UserModel getUserByUsername(RealmModel realm, String username) {
    log.info("getUserByUsername: " + username);
    TypedQuery<CustomUser> query = em.createNamedQuery("getUserByUsername", CustomUser.class);
    query.setParameter("username", username);
    List<CustomUser> result = query.getResultList();
    if (result.isEmpty()) {
      log.info("could not find username: " + username);
      return null;
    }

    return new UserAdapter(session, realm, model, result.get(0));
  }

  @Override
  public UserModel getUserByEmail(RealmModel realm, String email) {
    TypedQuery<CustomUser> query = em.createNamedQuery("getUserByEmail", CustomUser.class);
    query.setParameter("email", email);
    List<CustomUser> result = query.getResultList();
    if (result.isEmpty())
      return null;
    return new UserAdapter(session, realm, model, result.get(0));
  }

  @Override
  public UserModel addUser(RealmModel realm, String username) {
    CustomUser entity = new CustomUser();
    entity.setId(UUID.randomUUID().toString());
    entity.setUsername(username);
    em.persist(entity);
    log.info("added user: " + username);
    return new UserAdapter(session, realm, model, entity);
  }

  @Override
  public boolean removeUser(RealmModel realm, UserModel user) {
    String persistenceId = StorageId.externalId(user.getId());
    CustomUser entity = em.find(CustomUser.class, persistenceId);
    if (entity == null)
      return false;
    em.remove(entity);
    return true;
  }

  @Override
  public void onCache(RealmModel realm, CachedUserModel user, UserModel delegate) {
    String password = ((UserAdapter) delegate).getPassword();
    if (password != null) {
      user.getCachedWith().put(PASSWORD_CACHE_KEY, password);
    }
  }

  @Override
  public boolean supportsCredentialType(String credentialType) {
    return PasswordCredentialModel.TYPE.equals(credentialType);
  }

  @Override
  public boolean updateCredential(RealmModel realm, UserModel user, CredentialInput input) {
    if (!supportsCredentialType(input.getType()) || !(input instanceof UserCredentialModel cred)) {
      return false;
    }
    UserAdapter adapter = getUserAdapter(user);
    adapter.setPassword(cred.getValue());

    return true;
  }

  public UserAdapter getUserAdapter(UserModel user) {
    if (user instanceof CachedUserModel) {
      return (UserAdapter) ((CachedUserModel) user).getDelegateForUpdate();
    }
    return (UserAdapter) user;
  }

  @Override
  public void disableCredentialType(RealmModel realm, UserModel user, String credentialType) {
    if (!supportsCredentialType(credentialType)) {
      return;
    }
    getUserAdapter(user).setPassword(null);
  }

  @Override
  public Stream<String> getDisableableCredentialTypesStream(RealmModel realm, UserModel user) {
    if (getUserAdapter(user).getPassword() != null) {
      Set<String> set = new HashSet<>();
      set.add(PasswordCredentialModel.TYPE);
      return set.stream();
    }
    return Stream.empty();
  }

  @Override
  public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
    return supportsCredentialType(credentialType) && getPassword(user) != null;
  }

  @Override
  public boolean isValid(RealmModel realm, UserModel user, CredentialInput input) {
    if (!supportsCredentialType(input.getType()) || !(input instanceof UserCredentialModel cred)) {
      return false;
    }
    String password = getPassword(user);
    return password != null && password.equals(cred.getValue());
  }

  public String getPassword(UserModel user) {
    String password = null;
    if (user instanceof CachedUserModel) {
      password = (String) ((CachedUserModel) user).getCachedWith().get(PASSWORD_CACHE_KEY);
    } else if (user instanceof UserAdapter) {
      password = ((UserAdapter) user).getPassword();
    }
    return password;
  }

  @Override
  public int getUsersCount(RealmModel realm) {
    Object count = em.createNamedQuery("getUserCount")
        .getSingleResult();
    return ((Number) count).intValue();
  }

  @Override
  public Stream<UserModel> searchForUserStream(RealmModel realm, Map<String, String> params, Integer firstResult,
                                               Integer maxResults) {
    String search = params.get(UserModel.SEARCH);
    TypedQuery<CustomUser> query = searchUsers(search);

    if (firstResult != null) {
      query.setFirstResult(firstResult);
    }
    if (maxResults != null) {
      query.setMaxResults(maxResults);
    }
    return query.getResultStream().map(entity -> new UserAdapter(session, realm, model, entity));
  }

  @Override public Stream<UserModel> searchForUserStream(RealmModel realm, Map<String, String> params) {
    String search = params.get(UserModel.SEARCH);
    TypedQuery<CustomUser> query = searchUsers(search);

    return query.getResultStream().map(entity -> new UserAdapter(session, realm, model, entity));
  }

  @Override
  public Stream<UserModel> getGroupMembersStream(RealmModel realm, GroupModel group, Integer firstResult,
                                                 Integer maxResults) {
    return Stream.empty();
  }

  @Override
  public Stream<UserModel> searchForUserByUserAttributeStream(RealmModel realm, String attrName, String attrValue) {
    return Stream.empty();
  }

  private TypedQuery<CustomUser> searchUsers(String search) {
    TypedQuery<CustomUser> query;
    if (StringUtils.isEmpty(search)) {
      query = em.createNamedQuery("getAllUsers", CustomUser.class);
    } else {
      query = em.createNamedQuery("searchForUser", CustomUser.class);
      query.setParameter("search", "%" + search.toLowerCase() + "%");
    }
    return query;
  }


}
