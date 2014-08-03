package voteutil


type TemplateElectionMethod struct {

}

// Add a vote to this instance.
// ElectionMethod interface
func (it *TemplateElectionMethod) Vote(vote NameVote) {
}

// Add a vote to this instance.
// ElectionMethod interface
func (it *TemplateElectionMethod) VoteIndexes(vote IndexVote) {
}

// Get sorted result for the choices, and the number of winners (may be >1 if there is a tie.
// ElectionMethod interface
func (it *TemplateElectionMethod) GetResult() (*NameVote, int) {
	return nil, 0
}

// Return HTML explaining the result.
// ElectionMethod interface
func (it *TemplateElectionMethod) HtmlExlpaination() string {
	return ""
}

// Set shared NameMap
// ElectionMethod interface
func (it *TemplateElectionMethod) SetSharedNameMap(names *NameMap) {
}

// simple tag, lower case, no spaces
// ElectionMethod interface
func (it *TemplateElectionMethod) ShortName() string {
	return "vrr"
}

// Set the number of desired winners
// MultiSeat interface
func (it *TemplateElectionMethod) SetSeats(seats int) {
}
