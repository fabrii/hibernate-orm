/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.metamodel.model.domain.PersistentAttribute;
import org.hibernate.spi.NavigablePath;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaPredicate;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmJoinable;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.spi.SqmCreationHelper;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.type.descriptor.java.JavaType;

import org.jboss.logging.Logger;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

/**
 * Models a join based on a mapped attribute reference.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractSqmAttributeJoin<O,T>
		extends AbstractSqmJoin<O,T>
		implements SqmAttributeJoin<O,T> {
	private static final Logger log = Logger.getLogger( AbstractSqmAttributeJoin.class );

	private final boolean fetched;

	private SqmPredicate onClausePredicate;

	public AbstractSqmAttributeJoin(
			SqmFrom<?,O> lhs,
			SqmJoinable joinedNavigable,
			String alias,
			SqmJoinType joinType,
			boolean fetched,
			NodeBuilder nodeBuilder) {
		this(
				lhs,
				SqmCreationHelper.buildSubNavigablePath( lhs, joinedNavigable.getName(), alias ),
				joinedNavigable,
				alias == SqmCreationHelper.IMPLICIT_ALIAS ? null : alias,
				joinType,
				fetched,
				nodeBuilder
		);
	}

	protected AbstractSqmAttributeJoin(
			SqmFrom<?,O> lhs,
			NavigablePath navigablePath,
			SqmJoinable joinedNavigable,
			String alias,
			SqmJoinType joinType,
			boolean fetched,
			NodeBuilder nodeBuilder) {
		//noinspection unchecked
		super(
				navigablePath,
				(SqmPathSource<T>) joinedNavigable,
				lhs,
				alias,
				joinType,
				nodeBuilder
		);
		this.fetched = fetched;
	}

	protected void copyTo(AbstractSqmAttributeJoin<O, T> target, SqmCopyContext context) {
		super.copyTo( target, context );
		target.onClausePredicate = onClausePredicate == null ? null : onClausePredicate.copy( context );
	}

	@Override
	public SqmFrom<?, O> getLhs() {
		//noinspection unchecked
		return (SqmFrom<?, O>) super.getLhs();
	}

	@Override
	public JavaType<T> getNodeJavaType() {
		return getJavaTypeDescriptor();
	}

	public boolean isFetched() {
		return fetched;
	}

	@Override
	public SqmPredicate getJoinPredicate() {
		return onClausePredicate;
	}

	public void setJoinPredicate(SqmPredicate predicate) {
		if ( log.isTraceEnabled() ) {
			log.tracef(
					"Setting join predicate [%s] (was [%s])",
					predicate.toString(),
					this.onClausePredicate == null ? "<null>" : this.onClausePredicate.toString()
			);
		}

		this.onClausePredicate = predicate;
	}

	public void applyRestriction(SqmPredicate restriction) {
		if ( this.onClausePredicate == null ) {
			this.onClausePredicate = restriction;
		}
		else {
			this.onClausePredicate = nodeBuilder().and( onClausePredicate, restriction );
		}
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitQualifiedAttributeJoin( this );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JPA

	@Override
	public PersistentAttribute<? super O, ?> getAttribute() {
		//noinspection unchecked
		return (PersistentAttribute<? super O, ?>) getReferencedPathSource();
	}

	@Override
	public SqmAttributeJoin<O, T> on(JpaExpression<Boolean> restriction) {
		applyRestriction( nodeBuilder().wrap( restriction ) );
		return this;
	}

	@Override
	public SqmAttributeJoin<O, T> on(Expression<Boolean> restriction) {
		applyRestriction( nodeBuilder().wrap( restriction ) );
		return this;
	}

	@Override
	public SqmAttributeJoin<O, T> on(JpaPredicate... restrictions) {
		applyRestriction( nodeBuilder().wrap( restrictions ) );
		return this;
	}

	@Override
	public SqmAttributeJoin<O, T> on(Predicate... restrictions) {
		applyRestriction( nodeBuilder().wrap( restrictions ) );
		return this;
	}

	@Override
	public Predicate getOn() {
		return getJoinPredicate();
	}

	@Override
	public SqmFrom<?, O> getParent() {
		return getLhs();
	}

	@Override
	public JoinType getJoinType() {
		return getSqmJoinType().getCorrespondingJpaJoinType();
	}

}
