class Mark < ActiveRecord::Base

  belongs_to :equity, :foreign_key => :tradeable_id
 
  MARK_TYPES = [ ['Close', 'C'], ['Intra-Day', 'I'] ]

  validates_uniqueness_of :mark_date, :scope => :tradeable_id, 
    :message => "Already have a mark on that date. Please update an existing mark instead."

  validates_numericality_of(:mark_value, :message => "should be a number.")
  
  def validate
    errors.add(:mark_value, "should be a zero or positive value.") unless (!mark_value.blank? && mark_value >= 0)
    errors.add(:symbol, "cannot be empty.") unless !equity_m_symbol_root.blank?
    errors.add(:mark_date, "should not be in the future.") unless (!mark_date.blank? && (mark_date <= Date.today))
  end

  def before_create()
    if (self.mark_date.nil?)
      self.mark_date = Date::today()
    end
  end

  def equity_m_symbol_root
      (self.equity.nil? || self.equity.m_symbol.nil?) ? nil : self.equity.m_symbol.root
  end

  # pretty-print the errors
  def pretty_print_errors
    error_str = "{"
    if(errors.length > 0)
      if(!errors[:symbol].blank?)
        error_str += "Symbol #{errors[:symbol]} "
      end

      # mark_value can have 2 errors if it's not a number, so check if the erros[:mark_value] returns an Array 
      if(!errors[:mark_value].blank?)
        if(errors[:mark_value].instance_of?(Array))
          error_str += "Value #{errors[:mark_value][0]} "
        else
          error_str += "Value #{errors[:mark_value]} "
        end
      end
      if(!errors[:mark_date].blank?)
        error_str += "Date #{errors[:mark_date]} "
      end
    end
    error_str += "}"
  end
end
