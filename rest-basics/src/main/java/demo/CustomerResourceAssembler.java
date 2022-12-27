package demo;

import java.net.URI;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

@Component
class CustomerResourceAssembler implements RepresentationModelAssembler<Customer, EntityModel<Customer>> {

 @Override
 public EntityModel<Customer> toModel(Customer entity) {
  EntityModel<Customer> customerResource = new EntityModel<>(entity);//<1>
  URI photoUri = MvcUriComponentsBuilder
          .fromMethodCall(
                  MvcUriComponentsBuilder.on(CustomerProfilePhotoRestController.class).read(
                          entity.getId())).buildAndExpand().toUri();

  URI selfUri = MvcUriComponentsBuilder
          .fromMethodCall(
                  MvcUriComponentsBuilder.on(CustomerHypermediaRestController.class).get(
                          entity.getId())).buildAndExpand().toUri();

  customerResource.add(new Link(selfUri.toString(), "self"));
  customerResource.add(new Link(photoUri.toString(), "profile-photo"));
  return customerResource;
 }
}
