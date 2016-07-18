package org.incode.module.classification.dom.impl.category.taxonomy;

import lombok.Getter;
import lombok.Setter;
import org.apache.isis.applib.annotation.*;
import org.apache.isis.applib.annotation.Collection;
import org.apache.isis.applib.services.i18n.TranslatableString;
import org.incode.module.classification.dom.impl.applicability.Applicability;
import org.incode.module.classification.dom.impl.category.Category;
import org.incode.module.classification.dom.impl.category.CategoryRepository;

import javax.inject.Inject;
import javax.jdo.annotations.InheritanceStrategy;
import java.util.*;
import java.util.Optional;

@javax.jdo.annotations.PersistenceCapable
@javax.jdo.annotations.Inheritance(strategy = InheritanceStrategy.SUPERCLASS_TABLE)
@DomainObject(
        editing = Editing.DISABLED
)
public class Taxonomy extends Category {

    //region > constructor
    public Taxonomy(final String name) {
        super(null, name, null, 1);
    }
    //endregion


    //region > applicabilties (collection)
    public static class ApplicabilitiesDomainEvent extends CollectionDomainEvent<Applicability> { }
    @javax.jdo.annotations.Persistent(mappedBy = "taxonomy", dependentElement = "true")
    @Collection(
            domainEvent = ApplicabilitiesDomainEvent.class,
            editing = Editing.DISABLED
    )
    @Getter @Setter
    private SortedSet<Applicability> appliesTo = new TreeSet<>();

    //endregion

    //region > applicable (action)
    public static class ApplicableToDomainEvent extends ActionDomainEvent { }
    @Action(
            domainEvent = ApplicableToDomainEvent.class
    )
    @ActionLayout(
            cssClassFa = "fa-plus"
    )
    @MemberOrder(name = "appliesTo", sequence = "1")
    public Category applicable(final String atPath, final Class<?> domainType) {
        String domainTypeName = domainType.getName();
        Applicability applicability = new Applicability(this, atPath, domainTypeName);
        repositoryService.persistAndFlush(applicability);
        return this;
    }
    public TranslatableString validateApplicable(final String atPath, final Class<?> domainType) {
        String domainTypeName = domainType.getName();
        final Optional<Applicability> any =
                getAppliesTo().stream()
                        .filter(x -> Objects.equals(x.getAtPath(), atPath) &&
                                Objects.equals(x.getDomainType(), domainTypeName))
                        .findAny();
        return any.isPresent()
                ? TranslatableString.tr(
                        "Already applicable for '{atPath}' and '{domainTypeName}'",
                        "atPath", atPath,
                        "domainTypeName", domainTypeName)
                : null;
    }
    //endregion

    //region > notApplicable (action)
    public static class NotApplicableDomainEvent extends ActionDomainEvent { }
    @Action(
            domainEvent = NotApplicableDomainEvent.class,
            semantics = SemanticsOf.IDEMPOTENT_ARE_YOU_SURE
    )
    @ActionLayout(
            cssClassFa = "fa-minus"
    )
    @MemberOrder(name = "appliesTo", sequence = "2")
    public Category notApplicable(final Applicability applicability) {
        repositoryService.remove(applicability);
        return this;
    }

    public SortedSet<Applicability> choices0NotApplicable() {
        return getAppliesTo();
    }
    //endregion

    //region > injected
    @Inject
    CategoryRepository categoryRepository;
    //endregion
}

