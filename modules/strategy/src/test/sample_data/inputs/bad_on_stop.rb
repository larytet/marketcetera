require 'java'
java_import org.marketcetera.strategy.ruby.Strategy

######################################
# Used in RubyLanguageTest to ensure LogEvents for RubyExceptions
# are serializable
######################################
class BadOnStop < Strategy

  ####################################
  # Executed when the strategy is stopped.
  # 
  # The notify_high method should have 2 arguments, 
  # so will throw an error here and emit a LogEvent
  # 
  # This will need to be changed if notify_high is
  # ever modified to accept a single argument
  ####################################
  def on_stop 
    notify_high("word hummingbird"); # Throws error
  end
end
