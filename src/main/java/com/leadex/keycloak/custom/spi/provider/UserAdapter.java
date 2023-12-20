package com.leadex.keycloak.custom.spi.provider;

import com.leadex.keycloak.custom.spi.provider.model.CustomUser;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.adapter.AbstractUserAdapterFederatedStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class UserAdapter extends AbstractUserAdapterFederatedStorage {

  private static final Logger log = LoggerFactory.getLogger(UserAdapter.class);
  
  protected CustomUser entity;
  protected String keycloakId;

  public UserAdapter(KeycloakSession session, RealmModel realm, ComponentModel model, CustomUser entity) {
    super(session, realm, model);
    this.entity = entity;
    keycloakId = StorageId.keycloakId(model, entity.getId());
  }

  public String getPassword() {
    return entity.getPassword();
  }

  public void setPassword(String password) {
    entity.setPassword(password);
  }

  @Override
  public String getUsername() {
    return entity.getUsername();
  }

  @Override
  public void setUsername(String username) {
    entity.setUsername(username);

  }

  @Override
  public void setEmail(String email) {
    entity.setEmail(email);
  }

  @Override
  public String getEmail() {
    return entity.getEmail();
  }

  @Override
  public String getId() {
    return keycloakId;
  }

  @Override
  public void setSingleAttribute(String name, String value) {
    if (name.equals(AppConst.User.PHONE)) {
      entity.setPhone(value);
    } else {
      super.setSingleAttribute(name, value);
    }
  }

  @Override
  public void removeAttribute(String name) {
    if (name.equals(AppConst.User.PHONE)) {
      entity.setPhone(null);
    } else {
      super.removeAttribute(name);
    }
  }

  @Override
  public void setAttribute(String name, List<String> values) {
    if (name.equals(AppConst.User.PHONE)) {
      entity.setPhone(values.get(0));
    } else {
      super.setAttribute(name, values);
    }
  }

  @Override
  public String getFirstAttribute(String name) {
    if (name.equals(AppConst.User.PHONE)) {
      return entity.getPhone();
    } else {
      return super.getFirstAttribute(name);
    }
  }

  @Override
  public Map<String, List<String>> getAttributes() {
    Map<String, List<String>> attrs = super.getAttributes();
    MultivaluedHashMap<String, String> all = new MultivaluedHashMap<>();
    all.putAll(attrs);
    all.add(AppConst.User.PHONE, entity.getPhone());
    return all;
  }

  @Override
  public Stream<String> getAttributeStream(String name) {
    if (name.equals(AppConst.User.PHONE)) {
      List<String> phone = new LinkedList<>();
      phone.add(entity.getPhone());
      return phone.stream();
    } else {
      return super.getAttributeStream(name);
    }
  }

}
