package org.marketcetera.datamodel;

import org.marketcetera.core.ClassVersion;

import javax.persistence.*;
import java.math.BigDecimal;

/**
 * Representation of the Posting table
 * @author toli
 * @version $Id$
 */

@ClassVersion("$Id$")
@Entity
@Table(name = "postings")
public class Posting extends TableBase {

    @ManyToOne(optional = false)
    @JoinColumn(name = "sub_account_id")
    private SubAccount subAccount;

    @ManyToOne(optional = false)
    @JoinColumn(name = "journal_id")
    private Journal journal;

    @ManyToOne(optional = false)
    @JoinColumn(name="currency_id")
    private Currency currency;

    @Column(columnDefinition = "DECIMAL(20,5)")
    private BigDecimal quantity;

    public Posting() {
    }

    public Posting(SubAccount subAccount, Journal journal, Currency currency, BigDecimal quantity) {
        this.subAccount = subAccount;
        this.journal = journal;
        this.currency = currency;
        this.quantity = quantity;
    }

    public SubAccount getSubAccount() {
        return subAccount;
    }

    public void setSubAccount(SubAccount subAccount) {
        this.subAccount = subAccount;
    }

    public Journal getJournal() {
        return journal;
    }

    public void setJournal(Journal journal) {
        this.journal = journal;
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }
}
