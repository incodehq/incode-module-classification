package org.incode.module.classification.dom.impl.category;

import com.google.common.eventbus.Subscribe;
import lombok.Getter;
import lombok.Setter;
import org.apache.isis.applib.AbstractSubscriber;
import org.apache.isis.applib.annotation.*;
import org.apache.isis.applib.services.repository.RepositoryService;
import org.apache.isis.applib.util.ObjectContracts;
import org.incode.module.classification.dom.ClassificationModule;
import org.incode.module.classification.dom.impl.category.taxonomy.Taxonomy;

import javax.inject.Inject;
import javax.jdo.annotations.*;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;

@javax.jdo.annotations.PersistenceCapable(
        schema = "incodeClassification",
        table = "Category",
        identityType = IdentityType.DATASTORE)
@javax.jdo.annotations.DatastoreIdentity(
        strategy = IdGeneratorStrategy.NATIVE,
        column = "id")
@javax.jdo.annotations.Version(
        strategy = VersionStrategy.VERSION_NUMBER,
        column = "version")
@javax.jdo.annotations.Discriminator(strategy = DiscriminatorStrategy.CLASS_NAME) // can just check if has a parent
@javax.jdo.annotations.Queries({
        @javax.jdo.annotations.Query(
                name = "findByFullyQualifiedName", language = "JDOQL",
                value = "SELECT "
                        + "FROM org.incode.module.classification.dom.impl.category.Category "
                        + "WHERE fullyQualifiedName == :fullyQualifiedName"),
        @javax.jdo.annotations.Query(
                name = "findByTaxonomy", language = "JDOQL",
                value = "SELECT "
                        + "FROM org.incode.module.classification.dom.impl.category.Taxonomy "
                        + "WHERE taxonomy == :taxonomy"),
        @javax.jdo.annotations.Query(
                name = "findByParent", language = "JDOQL",
                value = "SELECT "
                        + "FROM org.incode.module.classification.dom.impl.category.Category "
                        + "WHERE parent == :parent"),
        @javax.jdo.annotations.Query(
                name = "findByParentAndName", language = "JDOQL",
                value = "SELECT "
                        + "FROM org.incode.module.classification.dom.impl.category.Category "
                        + "WHERE parent == :parent "
                        + "&&    name == :name "),
        @javax.jdo.annotations.Query(
                name = "findByParentAndReference", language = "JDOQL",
                value = "SELECT "
                        + "FROM org.incode.module.classification.dom.impl.category.Category "
                        + "WHERE parent == :parent "
                        + "&&    reference == :reference "),
})
@javax.jdo.annotations.Uniques({
        @javax.jdo.annotations.Unique(
                name="Classification_fullyQualifiedName_UNQ",
                members = { "fullyQualifiedName" }),
        @javax.jdo.annotations.Unique(
                name="Classification_parent_Name_UNQ",
                members = { "parent", "name" })
})
@DomainObject(
        editing = Editing.DISABLED
)
@DomainObjectLayout(
        titleUiEvent = Category.TitleUiEvent.class,
        iconUiEvent = Category.IconUiEvent.class,
        cssClassUiEvent = Category.CssClassUiEvent.class
)
public class Category implements Comparable<Category> {

    //region > ui event classes
    public static class TitleUiEvent extends ClassificationModule.TitleUiEvent<Category>{}
    public static class IconUiEvent extends ClassificationModule.IconUiEvent<Category>{}
    public static class CssClassUiEvent extends ClassificationModule.CssClassUiEvent<Category>{}
    //endregion

    //region > event classes
    public static abstract class PropertyDomainEvent<T> extends ClassificationModule.PropertyDomainEvent<Category, T> { }
    public static abstract class CollectionDomainEvent<T> extends ClassificationModule.CollectionDomainEvent<Category, T> { }
    public static abstract class ActionDomainEvent extends ClassificationModule.ActionDomainEvent<Category> { }
    //endregion

    //region > title, icon, cssClass
    /**
     * Implemented as a subscriber so can be overridden by consuming application if required.
     */
    @DomainService(nature = NatureOfService.DOMAIN)
    public static class TitleSubscriber extends AbstractSubscriber {
        @Subscribe
        public void on(Category.TitleUiEvent ev) {
            if(ev.getTitle() != null) {
                return;
            }
            ev.setTitle(titleOf(ev.getSource()));
        }
        private String titleOf(final Category category) {
            return category.getFullyQualifiedName() +
                    (category.getReference() != null
                            ? " [" + category.getReference() + "]"
                            : "");
        }
    }

    @DomainService
    public static class IconSubscriber extends AbstractSubscriber {
        @Subscribe
        public void on(Category.IconUiEvent ev) {
            if(ev.getIconName() != null) {
                return;
            }
            ev.setIconName("");
        }
    }

    /**
     * Implemented as a subscriber so can be overridden by consuming application if required.
     */
    @DomainService
    public static class CssClassSubscriber extends AbstractSubscriber {
        @Subscribe
        public void on(Category.CssClassUiEvent ev) {
            if(ev.getCssClass() != null) {
                return;
            }
            ev.setCssClass("");
        }
    }
    //endregion


    //region > constructor
    protected Category(Taxonomy taxonomy, final Category parent, final String name) {
        setTaxonomy(taxonomy);
        setParent(parent);
        setName(name);

        deriveFullyQualifiedName();
    }

    private void deriveFullyQualifiedName() {
        StringBuilder buf = new StringBuilder();
        prependName(this, buf);
        setFullyQualifiedName(buf.toString());
    }

    private static void prependName(Category category, final StringBuilder buf) {
        while(category != null) {
            prependNameOf(category, buf);
            category = category.getParent();
        }
    }

    private static void prependNameOf(final Category category, final StringBuilder buf) {
        if(buf.length() > 0) {
            buf.insert(0, "/");
        }
        buf.insert(0, category.getName());
    }
    //endregion


    //region > taxonomy (property)
    public static class TaxonomyDomainEvent extends PropertyDomainEvent<Category> { }
    @Column(allowsNull = "true", name = "taxonomyId") // conceptually, not-null; however a taxonomy will refer to itself
    @Property(domainEvent = TaxonomyDomainEvent.class)
    @Getter @Setter
    private Taxonomy taxonomy;
    //endregion

    //region > fullyQualifiedName (derived property, persisted)
    public static class FullyQualifiedNameDomainEvent extends PropertyDomainEvent<String> { }
    @Title
    @Getter @Setter
    @javax.jdo.annotations.Column(allowsNull = "false", length = ClassificationModule.JdoColumnLength.CATEGORY_FQNAME)
    @Property(domainEvent = FullyQualifiedNameDomainEvent.class)
    private String fullyQualifiedName;
    //endregion

    //region > parent (property)
    public static class ParentDomainEvent extends PropertyDomainEvent<Category> { }
    @Column(allowsNull = "true", name = "parentId")
    @Property(domainEvent = ParentDomainEvent.class)
    @Getter @Setter
    private Category parent;

    public boolean hideParent() {
        return getParent() == getTaxonomy();
    }
    //endregion

    //region > name (property)
    public static class NameDomainEvent extends PropertyDomainEvent<String> { }
    @Getter @Setter
    @javax.jdo.annotations.Column(allowsNull = "false", length = ClassificationModule.JdoColumnLength.CATEGORY_NAME)
    @Property(domainEvent = NameDomainEvent.class)
    private String name;
    //endregion

    //region > reference (property)
    public static class ReferenceDomainEvent extends PropertyDomainEvent<String> { }

    /**
     * Optional reference.
     */
    @Getter @Setter
    @javax.jdo.annotations.Column(allowsNull = "true", length = ClassificationModule.JdoColumnLength.CATEGORY_REFERENCE)
    @Property(domainEvent = ReferenceDomainEvent.class)
    private String reference;
    //endregion


    //region > children (property)
    @Persistent(mappedBy = "parent", dependentElement = "false")
    @Collection()
    @Getter @Setter
    private SortedSet<Category> children = new TreeSet<>();
    //endregion

    //region > addChild (action)

    @Action()
    @ActionLayout(
            cssClassFa = "fa-plus",
            named = "Add"
    )
    @MemberOrder(name = "children", sequence = "1")
    public Category addChild(@ParameterLayout(named="Name") final String localName) {
        Category category = new Category(taxonomy, this, localName);
        repositoryService.persistAndFlush(category);
        return category;
    }

    public String validate0AddChild(final String localName) {
        final Optional<Category> any =
                getChildren().stream().filter(x -> Objects.equals(x.getName(), localName)).findAny();
        return any.isPresent() ? "There is already a child classification with the name of '" + localName + "'": null;
    }
    // endregion

    //region > removeChild (action)

    @Action(semantics = SemanticsOf.IDEMPOTENT_ARE_YOU_SURE)
    @ActionLayout(
            cssClassFa = "fa-minus",
            named = "Remove"
    )
    @MemberOrder(name = "children", sequence = "2")
    public Category removeChild(final Category category) {
        removeCascade(category);
        return this;
    }

    public java.util.Collection<Category> choices0RemoveChild() {
        return getChildren();
    }

    private void removeCascade(final Category category) {
        SortedSet<Category> children = category.getChildren();
        for (Category child : children) {
            removeCascade(child);
        }
        repositoryService.remove(category);
    }

    // endregion


    //region > toString, compareTo

    @Override
    public String toString() {
        return ObjectContracts.toString(this, "fullyQualifiedName");
    }

    @Override
    public int compareTo(final Category other) {
        return ObjectContracts.compare(this, other, "fullyQualifiedName");
    }

    //endregion

    //region > injected services

    @Inject
    protected
    RepositoryService repositoryService;

    //endregion


}