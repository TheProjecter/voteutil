package voteutil

import "errors"
import "sort"
import "strconv"
import "net/url"

type NameRating struct {
	Name   string
	Rating float64
}

//type NameVote map[string] float64
//type IndexVote map[int] float64
type NameVote []NameRating

// IndexVote is perhaps excessively optimized to pack nicely.
//
// You might think that a slice of struct of {int,float} would be nicer, and
// you'd be right, but (int32,float64) will leave 4 bytes of dead space when you
// make an array of them. int[],float64[] will pack more efficiently.
type IndexVote struct {
	Indexes []int
	Ratings []float64
}

func NewIndexVote(size int) *IndexVote {
	out := new(IndexVote)
	out.Indexes = make([]int, size)
	out.Ratings = make([]float64, size)
	return out
}

// In-place sort
func (it *NameVote) Sort() {
	sort.Sort(it)
}

// sort.Interface
func (it *NameVote) Len() int {
	return len(*it)
}
func (it *NameVote) Less(i, j int) bool {
	return (*it)[i].Rating > (*it)[j].Rating
}
func (it *NameVote) Swap(i, j int) {
	t := (*it)[i]
	(*it)[i] = (*it)[j]
	(*it)[j] = t
}

// Map from names to indexes.
// The reverse should be kept as an array of string.
type NameMap struct {
	Indexes map[string]int
	Names   []string
}

func (nm *NameMap) NameToIndex(name string) int {
	if nm.Indexes == nil {
		nm.Indexes = make(map[string]int)
	}
	x, ok := nm.Indexes[name]
	if ok {
		return x
	}
	x = len(nm.Names)
	nm.Indexes[name] = x
	nm.Names = append(nm.Names, name)
	return x
}

func (nm *NameMap) IndexToName(x int) string {
	return nm.Names[x]
}

func (nm *NameMap) NameVoteToIndexVote(it NameVote) *IndexVote {
	//out := new(IndexVote)
	out := NewIndexVote(len(it))
	i := 0
	for _, nv := range it {
		out.Indexes[i] = nm.NameToIndex(nv.Name)
		out.Ratings[i] = nv.Rating
		i++
	}
	return out
}

// Parse url-query formatted vote.
// choice+one=1&choice+2=2&...
func UrlToNameVote(vote string) (*NameVote, error) {
	vals, err := url.ParseQuery(vote)
	if err != nil {
		return nil, err
	}
	out := new(NameVote)
	for name, slist := range vals {
		if len(slist) != 1 {
			return nil, errors.New("Too many values for name: " + name)
		}
		rating, err := strconv.ParseFloat(slist[0], 64)
		if err != nil {
			return nil, err
		}
		*out = append(*out, NameRating{name, rating})
	}
	return out, nil
}

type ElectionMethod interface {
	// Add a vote to this instance.
	Vote(NameVote)

	// Add a vote to this instance.
	VoteIndexes(IndexVote)

	// Get sorted result for the choices, and the number of winners (may be >1 if there is a tie.
	GetResult() (scores *NameVote, numWinners int)

	// Return HTML explaining the result.
	HtmlExplaination() string

	// Set shared NameMap
	SetSharedNameMap(names *NameMap)

	// simple tag, lower case, no spaces
	ShortName() string
}

type MultiSeat interface {
	// Set the number of desired winners
	SetSeats(seats int)
}

/*
function ends without a return statement
func ReturnValue(foo bool) int {
	if foo {
		return 1
	} else {
		return 0
	}
}

*/
